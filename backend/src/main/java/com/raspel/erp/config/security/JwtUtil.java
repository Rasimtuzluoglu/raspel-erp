package com.raspel.erp.config.security;

import com.raspel.erp.entity.sistem.Kullanici;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret en az 32 bayt (256-bit) olmalıdır");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        if (System.getenv("JWT_SECRET") == null) {
            log.warn("JWT_SECRET ortam değişkeni tanımlı değil! Üretim ortamında mutlaka güçlü bir JWT_SECRET ayarlayın.");
        }
    }

    public String generateToken(Kullanici kullanici, Long sirketId, String sirketAdi) {
        return Jwts.builder()
                .subject(kullanici.getUsername())
                .claim("userId", kullanici.getId())
                .claim("role", kullanici.getRole())
                .claim("displayName", kullanici.getDisplayName())
                .claim("sirketId", sirketId != null ? sirketId : kullanici.getSirketId())
                .claim("sirketAdi", sirketAdi)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getPayload().getSubject();
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
