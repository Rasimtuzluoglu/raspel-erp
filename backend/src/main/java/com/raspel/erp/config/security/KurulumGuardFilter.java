package com.raspel.erp.config.security;

import com.raspel.erp.repository.sistem.SirketRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * İlk kurulum tamamlandıktan sonra yeniden kurulum girişimlerini engeller.
 * /api/kurulum/durum (salt okunur durum sorgusu, ön-giriş ekranında kullanılır) hariç
 * tüm /api/kurulum/** isteklerine kurulum tamamlanmışsa 403 döndürür. Böylece kurulum
 * sonrası uzına kadar herkese açık kalan yazma/yükleme yüzeyi kapanır.
 */
@Component
public class KurulumGuardFilter extends OncePerRequestFilter {

    @Autowired(required = false)
    private SirketRepository sirketRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean kurulumYolu = path.startsWith("/api/kurulum/") || path.equals("/api/kurulum");
        boolean durumOkuma = path.equals("/api/kurulum/durum");

        if (kurulumYolu && !durumOkuma && sirketRepository != null && sirketRepository.count() > 0) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Kurulum zaten tamamlanmis; bu uygulama giris ekraninda kullanilir\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}