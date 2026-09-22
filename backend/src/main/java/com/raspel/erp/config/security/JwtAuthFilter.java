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
import com.raspel.erp.service.sistem.AktifOturumService;
import com.raspel.erp.service.sistem.ApiTokenService;
import com.raspel.erp.entity.sistem.Kullanici;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final KullaniciRepository kullaniciRepository;
    private final AktifOturumService aktifOturumService;
    private final ApiTokenService apiTokenService;

    /** Prometheus scrape icin paylasilan token (tanimliysa). Bos ise scrape devre disi. */
    @org.springframework.beans.factory.annotation.Value("${app.metrics.scrape-token:}")
    private String metricsScrapeToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Prometheus scrape token'i: /actuator/prometheus icin PROMETHEUS rolu verir.
        if (metricsScrapeToken != null && !metricsScrapeToken.isBlank()
                && "/actuator/prometheus".equals(request.getRequestURI())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String gelen = request.getHeader("X-Metrics-Token");
            if (gelen == null) {
                String bearer = request.getHeader("Authorization");
                if (bearer != null && bearer.startsWith("Bearer ")) gelen = bearer.substring(7);
            }
            if (metricsScrapeToken.equals(gelen)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "prometheus", null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PROMETHEUS")));
                SecurityContextHolder.getContext().setAuthentication(auth);
                filterChain.doFilter(request, response);
                return;
            }
        }

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

        // Kişisel erişim token'ı (REST API entegrasyonu): raspel_pat_ önekiyle gelir.
        if (token != null && token.startsWith("raspel_pat_")) {
            Kullanici kullanici = apiTokenService.tokenIleKullaniciBul(token);
            if (kullanici != null && Boolean.TRUE.equals(kullanici.getActive())
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(kullanici.getUsername());
                if (userDetails != null && userDetails.isEnabled()) {
                    request.setAttribute("kullaniciId", kullanici.getId());
                    request.setAttribute("sirketId", kullanici.getSirketId());
                    request.setAttribute("displayName", kullanici.getDisplayName());
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    apiTokenService.sonKullanimGuncelle(token);
                }
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (token != null && jwtUtil.validateToken(token)) {
            // İptal edilmiş (sonlandırılmış) oturumların token'ı geçersizdir.
            request.setAttribute("jti", jwtUtil.getJtiFromToken(token));
            if (aktifOturumService.iptalEdilmis(jwtUtil.getJtiFromToken(token))) {
                filterChain.doFilter(request, response);
                return;
            }
            Long kullaniciId = jwtUtil.getUserIdFromToken(token);
            Long sirketId = jwtUtil.getSirketIdFromToken(token);
            // "En son giris kazanir": bu jti kullanicinin gecerli oturumu degilse reddedilir.
            if (!aktifOturumService.aktifOturumMu(kullaniciId, jwtUtil.getJtiFromToken(token))) {
                filterChain.doFilter(request, response);
                return;
            }
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
