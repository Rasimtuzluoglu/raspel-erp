package com.raspel.erp.config.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class LoginRateLimitFilter implements Filter {

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if ("/api/kullanicilar/giris".equals(req.getRequestURI()) && "POST".equalsIgnoreCase(req.getMethod())) {
            String ip = req.getRemoteAddr();
            LoginAttempt attempt = attempts.computeIfAbsent(ip, k -> new LoginAttempt());
            if (attempt.isBlocked()) {
                res.setStatus(429);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"message\":\"Çok fazla giriş denemesi. Lütfen 60 saniye bekleyin.\"}");
                return;
            }
            attempt.increment();
        }

        chain.doFilter(request, response);
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
