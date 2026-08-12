package com.raspel.erp.config.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
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
        String remoteAddr = req.getRemoteAddr();
        // X-Forwarded-For yalnızca güvenilir proxy'den (özel/loopback ağ) geldiğinde kullanılır.
        // Doğrudan internetten gelen isteklerde spoofing engellenir.
        if (xForwardedFor != null && !xForwardedFor.isBlank() && guvenilirProxyMu(remoteAddr)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return remoteAddr;
    }

    private boolean guvenilirProxyMu(String addr) {
        if (addr == null) return false;
        if (addr.equals("127.0.0.1") || addr.equals("::1") || addr.equals("0:0:0:0:0:0:0:1")) return true;
        if (addr.startsWith("10.")) return true;
        if (addr.startsWith("192.168.")) return true;
        // 172.16.0.0 - 172.31.255.255 (RFC 1918)
        if (addr.startsWith("172.")) {
            try {
                int ikinci = Integer.parseInt(addr.substring(4, addr.indexOf('.', 4)));
                return ikinci >= 16 && ikinci <= 31;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
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
