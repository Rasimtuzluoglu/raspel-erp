package com.raspel.erp.config.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class LoginRateLimitFilter implements Filter {

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000;

    private boolean girisYolu(String uri) {
        if (uri == null) return false;
        return uri.endsWith("/kullanicilar/giris") || uri.endsWith("/kullanicilar/giris-2fa");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (girisYolu(req.getRequestURI()) && "POST".equalsIgnoreCase(req.getMethod())) {
            String ip = getClientIp(req);
            LoginAttempt attempt = attempts.computeIfAbsent(ip, k -> new LoginAttempt());
            if (attempt.isBlocked()) {
                res.setStatus(429);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"message\":\"Çok fazla giriş denemesi. Lütfen 60 saniye bekleyin.\"}");
                return;
            }
            attempt.increment();
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Basarili giriste sayac sifirlanir
            if (girisYolu(req.getRequestURI()) && res.getStatus() >= 200 && res.getStatus() < 300) {
                attempts.remove(getClientIp(req));
            }
            temizle();
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    /** Süresi dolmuş kayıtları bellekten temizler. */
    private void temizle() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, LoginAttempt>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LoginAttempt> e = it.next();
            if (now - e.getValue().windowStart > WINDOW_MS) {
                it.remove();
            }
        }
    }

    private static class LoginAttempt {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        boolean isBlocked() {
            synchronized (this) {
                long now = System.currentTimeMillis();
                if (now - windowStart > WINDOW_MS) {
                    count.set(0);
                    windowStart = now;
                    return false;
                }
                return count.get() >= MAX_ATTEMPTS;
            }
        }

        void increment() {
            count.incrementAndGet();
        }
    }
}
