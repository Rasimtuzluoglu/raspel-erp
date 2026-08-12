package com.raspel.erp.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import com.raspel.erp.repository.sistem.KullaniciRepository;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final KullaniciRepository kullaniciRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if (token != null && jwtUtil.validateToken(token)) {
            Long kullaniciId = jwtUtil.getUserIdFromToken(token);
            Long sirketId = jwtUtil.getSirketIdFromToken(token);
            if (kullaniciId != null) {
                request.setAttribute("kullaniciId", kullaniciId);
            }
            if (sirketId != null) {
                request.setAttribute("sirketId", sirketId);
            }
            request.setAttribute("displayName", jwtUtil.getDisplayNameFromToken(token));

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtUtil.getUsernameFromToken(token);
                Long tokenVersion = jwtUtil.getTokenVersionFromToken(token);

                // Sifre degistirildiyse eski token'lar gecersizdir (tokenVersion kontrolu)
                boolean tokenGecerli = true;
                try {
                    Long dbVersion = kullaniciRepository.findByUsername(username)
                            .map(u -> u.getTokenVersion() != null ? u.getTokenVersion() : 0L)
                            .orElse(null);
                    if (dbVersion != null && tokenVersion != null && !dbVersion.equals(tokenVersion)) {
                        tokenGecerli = false;
                    }
                } catch (Exception ignored) {
                    // Kullanici bulunamazsa tokenGecerli kalir, loadUserByUsername asagida hata verir
                }

                if (!tokenGecerli) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (userDetails != null && userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
