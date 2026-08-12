package com.raspel.erp.config.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Giriş brute-force koruması.
 * Redis varsa paylaşımlı sayaç kullanır (çoklu instance), yoksa in-memory fallback.
 */
@Component
public class LoginRateLimitFilter implements Filter {

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000;
    private static final String REDIS_KEY_PREFIX = "login:rate:";

    private final StringRedisTemplate redisTemplate;

    public LoginRateLimitFilter(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisTemplate = redisProvider.getIfAvailable();
    }

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
            if (engellendiMi(ip)) {
                res.setStatus(429);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"message\":\"Çok fazla giriş denemesi. Lütfen 60 saniye bekleyin.\"}");
                return;
            }
            denemeKaydet(ip);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            // Basarili giriste sayac sifirlanir
            if (girisYolu(req.getRequestURI()) && res.getStatus() >= 200 && res.getStatus() < 300) {
                sifirla(getClientIp(req));
            }
            temizle();
        }
    }

    private boolean redisKullanilabilir() {
        if (redisTemplate == null) return false;
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean engellendiMi(String ip) {
        if (redisKullanilabilir()) {
            try {
                String v = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + ip);
                if (v != null) {
                    return Integer.parseInt(v) >= MAX_ATTEMPTS;
                }
                return false;
            } catch (Exception e) {
                // Redis hatasında in-memory fallback
            }
        }
        LoginAttempt attempt = attempts.get(ip);
        return attempt != null && attempt.isBlocked();
    }

    private void denemeKaydet(String ip) {
        if (redisKullanilabilir()) {
            try {
                redisTemplate.opsForValue().increment(REDIS_KEY_PREFIX + ip);
                redisTemplate.expire(REDIS_KEY_PREFIX + ip, java.time.Duration.ofMillis(WINDOW_MS));
                return;
            } catch (Exception e) {
                // Redis hatasında in-memory fallback
            }
        }
        attempts.computeIfAbsent(ip, k -> new LoginAttempt()).increment();
    }

    private void sifirla(String ip) {
        if (redisKullanilabilir()) {
            try {
                redisTemplate.delete(REDIS_KEY_PREFIX + ip);
            } catch (Exception ignored) { }
        }
        attempts.remove(ip);
    }

    private String getClientIp(HttpServletRequest req) {
        String xForwardedFor = req.getHeader("X-Forwarded-For");
        String remoteAddr = req.getRemoteAddr();
        // X-Forwarded-For yalnızca güvenilir proxy'den (özel/loopback ağ) geldiğinde kullanılır.
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

    /** Süresi dolmuş in-memory kayıtları temizler. */
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
