package com.raspel.erp.config.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Brute-force koruması: hem istemci IP'si hem de kullanıcı adı bazında sayaç tutar.
 * Redis varsa paylaşımlı (çoklu instance), yoksa in-memory fallback kullanılır.
 * Kapsam: giriş, 2FA tamamlama ve şifre sıfırlama uçları.
 */
@Component
public class LoginRateLimitFilter implements Filter {

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 60_000;
    private static final String REDIS_KEY_PREFIX = "login:rate:";
    private static final Pattern KULLANICI_ADI = Pattern.compile("\"username\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final StringRedisTemplate redisTemplate;

    public LoginRateLimitFilter(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisTemplate = redisProvider.getIfAvailable();
    }

    /** Sayaç tutulan POST uçları. */
    private boolean korumaliYol(String uri) {
        if (uri == null) return false;
        return uri.endsWith("/kullanicilar/giris")
                || uri.endsWith("/kullanicilar/giris-2fa")
                || uri.endsWith("/kullanicilar/giris-sirket")
                || uri.endsWith("/kullanicilar/sifre-sifirlama-talebi")
                || uri.endsWith("/kullanicilar/sifre-sifirlama-onayla");
    }

    private boolean girisYolu(String uri) {
        return uri != null && (uri.endsWith("/kullanicilar/giris") || uri.endsWith("/kullanicilar/giris-2fa"));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (korumaliYol(req.getRequestURI()) && "POST".equalsIgnoreCase(req.getMethod())) {
            String ip = getClientIp(req);
            String kullaniciAnahtari = null;
            HttpServletRequest zincirIstegi = req;

            if (girisYolu(req.getRequestURI())) {
                try {
                    TekrarOkunabilirIstek sarili = new TekrarOkunabilirIstek(req);
                    String kullaniciAdi = kullaniciAdiOku(sarili.govde());
                    if (kullaniciAdi != null) {
                        kullaniciAnahtari = REDIS_KEY_PREFIX + "u:" + sha256(kullaniciAdi);
                    }
                    zincirIstegi = sarili;
                } catch (Exception ignored) {
                    /* gövde okunamazsa yalnızca IP bazlı koruma uygulanır */
                }
            }

            if (engellendiMi(REDIS_KEY_PREFIX + ip) || (kullaniciAnahtari != null && engellendiMi(kullaniciAnahtari))) {
                res.setStatus(429);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"message\":\"Çok fazla deneme. Lütfen 60 saniye bekleyin.\"}");
                return;
            }
            denemeKaydet(REDIS_KEY_PREFIX + ip);

            try {
                chain.doFilter(zincirIstegi, response);
            } finally {
                boolean basarili = res.getStatus() >= 200 && res.getStatus() < 300;
                if (basarili) {
                    sifirla(REDIS_KEY_PREFIX + ip);
                    if (kullaniciAnahtari != null) sifirla(kullaniciAnahtari);
                } else if (kullaniciAnahtari != null) {
                    // Başarısız denemede kullanıcı adı sayacı artırılır (dağıtık brute-force engeli).
                    denemeKaydet(kullaniciAnahtari);
                }
                temizle();
            }
            return;
        }

        chain.doFilter(request, response);
    }

    /** JSON gövdesinden kullanıcı adını çıkarır (gövde tüketilmez, sarılı istek üzerinden). */
    private String kullaniciAdiOku(byte[] govde) {
        if (govde == null || govde.length == 0) return null;
        Matcher m = KULLANICI_ADI.matcher(new String(govde, StandardCharsets.UTF_8));
        if (!m.find()) return null;
        String ad = m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
        return ad.isBlank() ? null : ad.trim().toLowerCase();
    }

    private static String sha256(String deger) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] ozet = md.digest(deger.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(ozet.length * 2);
            for (byte b : ozet) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(deger.hashCode());
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

    private boolean engellendiMi(String anahtar) {
        if (redisKullanilabilir()) {
            try {
                String v = redisTemplate.opsForValue().get(anahtar);
                if (v != null) {
                    return Integer.parseInt(v) >= MAX_ATTEMPTS;
                }
                return false;
            } catch (Exception e) {
                // Redis hatasında in-memory fallback
            }
        }
        LoginAttempt attempt = attempts.get(anahtar);
        return attempt != null && attempt.isBlocked();
    }

    private void denemeKaydet(String anahtar) {
        if (redisKullanilabilir()) {
            try {
                redisTemplate.opsForValue().increment(anahtar);
                redisTemplate.expire(anahtar, java.time.Duration.ofMillis(WINDOW_MS));
                return;
            } catch (Exception e) {
                // Redis hatasında in-memory fallback
            }
        }
        attempts.computeIfAbsent(anahtar, k -> new LoginAttempt()).increment();
    }

    private void sifirla(String anahtar) {
        if (redisKullanilabilir()) {
            try {
                redisTemplate.delete(anahtar);
            } catch (Exception ignored) { }
        }
        attempts.remove(anahtar);
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

    /** Gövdesi birden fazla okunabilen istek sarıcısı (filtre + controller). */
    private static class TekrarOkunabilirIstek extends HttpServletRequestWrapper {
        private final byte[] govde;

        TekrarOkunabilirIstek(HttpServletRequest request) throws IOException {
            super(request);
            this.govde = request.getInputStream().readAllBytes();
        }

        byte[] govde() {
            return govde;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(govde);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return bais.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return bais.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return bais.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    /* senkron okuma */
                }
            };
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
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
