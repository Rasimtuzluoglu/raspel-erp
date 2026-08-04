package com.raspel.erp.config.security;

import com.raspel.erp.entity.sistem.Kullanici;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciB0aGUgand0IHRva2VuIGdlbmVyYXRpb24gYWxnb3JpdGhtIGluIHRoZSBhcHBsaWNhdGlvbiBmb3IgdGVzdGluZyBwdXJwb3Nlcw==";
    private static final long EXPIRATION_MS = 86400000L;

    private JwtUtil jwtUtil;
    private Kullanici testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
        testUser = new Kullanici();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setDisplayName("Test User");
        testUser.setRole("ADMIN");
        testUser.setActive(true);
        testUser.setOlusturmaTarihi(LocalDateTime.now());
    }

    @Test
    void generateToken_createsValidToken() {
        String token = jwtUtil.generateToken(testUser, null, "Test Company");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void getUsernameFromToken_returnsCorrectUsername() {
        String token = jwtUtil.generateToken(testUser, null, "Test Company");

        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals("testuser", username);
    }

    @Test
    void getUserIdFromToken_returnsCorrectUserId() {
        String token = jwtUtil.generateToken(testUser, null, "Test Company");

        Long userId = jwtUtil.getUserIdFromToken(token);

        assertEquals(1L, userId);
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = jwtUtil.generateToken(testUser, null, "Test Company");

        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_returnsFalseForMalformedToken() {
        boolean isValid = jwtUtil.validateToken("invalid.token.here");

        assertFalse(isValid);
    }

    @Test
    void validateToken_returnsFalseForEmptyToken() {
        boolean isValid = jwtUtil.validateToken("");

        assertFalse(isValid);
    }

    @Test
    void validateToken_returnsFalseForExpiredToken() throws Exception {
        JwtUtil shortLived = new JwtUtil(SECRET, 1L);
        String token = shortLived.generateToken(testUser, null, "Test Company");

        Thread.sleep(10);

        boolean isValid = jwtUtil.validateToken(token);
        assertFalse(isValid);
    }

    @Test
    void tokenContainsExpectedClaims() {
        String token = jwtUtil.generateToken(testUser, 10L, "My Company");

        SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Jws<Claims> claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);

        assertEquals("testuser", claims.getPayload().getSubject());
        assertEquals(1L, claims.getPayload().get("userId", Long.class).longValue());
        assertEquals("ADMIN", claims.getPayload().get("role"));
        assertEquals("Test User", claims.getPayload().get("displayName"));
        assertEquals(10L, claims.getPayload().get("sirketId", Long.class).longValue());
        assertEquals("My Company", claims.getPayload().get("sirketAdi"));
        assertNotNull(claims.getPayload().getIssuedAt());
        assertNotNull(claims.getPayload().getExpiration());
    }
}
