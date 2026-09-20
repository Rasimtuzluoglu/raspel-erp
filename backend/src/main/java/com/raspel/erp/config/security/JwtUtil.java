package com.raspel.erp.config.security;

import com.raspel.erp.entity.sistem.Kullanici;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            Environment env) {
        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
        byte[] keyBytes;
        if (jwtSecret == null || jwtSecret.isBlank()) {
            if (isProd) {
                throw new IllegalStateException(
                    "JWT_SECRET ortam degiskeni tanimli degil! Uretim ortaminda JWT_SECRET zorunludur.");
            }
            // Dev/test: kodda secret bulunmaz; restart'ta oturumlari gecersiz kilan gecici anahtar.
            log.warn("JWT_SECRET tanimli degil; gelistirme icin gecici (ephemeral) imza anahtari uretildi. " +
                    "Restart sonrasi mevcut oturumlar gecersiz olur. Uretimde mutlaka JWT_SECRET ayarlayin.");
            keyBytes = new java.security.SecureRandom().generateSeed(32);
        } else {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
            if (keyBytes.length < 32) {
                throw new IllegalStateException("app.jwt.secret en az 32 bayt (256-bit) olmalidir");
            }
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(Kullanici kullanici, Long sirketId, String sirketAdi) {
        return generateToken(kullanici, sirketId, sirketAdi, expirationMs);
    }

    /** Belirtilen gecerlilik suresiyle token uretir (oturum uzatma icin). */
    public String generateToken(Kullanici kullanici, Long sirketId, String sirketAdi, long gecerlilikMs) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(kullanici.getUsername())
                .claim("userId", kullanici.getId())
                .claim("role", kullanici.getRole())
                .claim("displayName", kullanici.getDisplayName())
                .claim("sirketId", sirketId != null ? sirketId : kullanici.getSirketId())
                .claim("sirketAdi", sirketAdi)
                .claim("tokenVersion", kullanici.getTokenVersion() != null ? kullanici.getTokenVersion() : 0L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + gecerlilikMs))
                .signWith(signingKey)
                .compact();
    }

    /** Token'in bitis zamanini (epoch ms) dondurur; okunamazsa null. */
    public Long getExpirationFromToken(String token) {
        try {
            return getClaims(token).getPayload().getExpiration().getTime();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getPayload().getSubject();
    }

    public String getJtiFromToken(String token) {
        return getClaims(token).getPayload().getId();
    }

    public Long getUserIdFromToken(String token) {
        Object val = getClaims(token).getPayload().get("userId");
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }

    public Long getSirketIdFromToken(String token) {
        Object val = getClaims(token).getPayload().get("sirketId");
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }

    public String getDisplayNameFromToken(String token) {
        Object val = getClaims(token).getPayload().get("displayName");
        return val != null ? val.toString() : null;
    }

    public Long getTokenVersionFromToken(String token) {
        Object val = getClaims(token).getPayload().get("tokenVersion");
        if (val instanceof Number) return ((Number) val).longValue();
        return null;
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }
}
