package com.raspel.erp.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ProdGuvenlikKontroluTest {

    private ProdGuvenlikKontrolu olustur(boolean prodAktif) {
        MockEnvironment env = new MockEnvironment();
        if (prodAktif) env.setActiveProfiles("prod");
        ProdGuvenlikKontrolu kontrol = new ProdGuvenlikKontrolu(env);
        ReflectionTestUtils.setField(kontrol, "jwtSecret", "bu-cok-uzun-ve-guclu-bir-jwt-secret-degeri-1234567890");
        ReflectionTestUtils.setField(kontrol, "aiEncryptionKey", "guclu-ai-anahtari-1234");
        ReflectionTestUtils.setField(kontrol, "storageType", "local");
        ReflectionTestUtils.setField(kontrol, "minioAccessKey", "");
        ReflectionTestUtils.setField(kontrol, "minioSecretKey", "");
        ReflectionTestUtils.setField(kontrol, "relayEnabled", false);
        ReflectionTestUtils.setField(kontrol, "rabbitmqPassword", "rabbit-guclu");
        ReflectionTestUtils.setField(kontrol, "dbPassword", "db-guclu-sifre");
        ReflectionTestUtils.setField(kontrol, "redisPassword", "redis-guclu-sifre");
        ReflectionTestUtils.setField(kontrol, "corsAllowedOrigins", "https://erp.raspel.com");
        ReflectionTestUtils.setField(kontrol, "frontendUrl", "https://erp.raspel.com");
        ReflectionTestUtils.setField(kontrol, "strictDomains", false);
        return kontrol;
    }

    @Test
    void devProfilindeKontrolYapilmaz() {
        ProdGuvenlikKontrolu kontrol = olustur(false);
        ReflectionTestUtils.setField(kontrol, "jwtSecret", "");
        assertDoesNotThrow(kontrol::kontrol);
    }

    @Test
    void prodGecerliAyarlarlaBasarili() {
        assertDoesNotThrow(() -> olustur(true).kontrol());
    }

    @Test
    void prodZayifJwtSecretReddedilir() {
        ProdGuvenlikKontrolu kontrol = olustur(true);
        ReflectionTestUtils.setField(kontrol, "jwtSecret", "kisa");
        assertThrows(IllegalStateException.class, kontrol::kontrol);
    }

    @Test
    void prodEksikAiAnahtariReddedilir() {
        ProdGuvenlikKontrolu kontrol = olustur(true);
        ReflectionTestUtils.setField(kontrol, "aiEncryptionKey", "");
        assertThrows(IllegalStateException.class, kontrol::kontrol);
    }

    @Test
    void prodBosDbSifresiReddedilir() {
        ProdGuvenlikKontrolu kontrol = olustur(true);
        ReflectionTestUtils.setField(kontrol, "dbPassword", "");
        assertThrows(IllegalStateException.class, kontrol::kontrol);
    }

    @Test
    void prodBosCorsReddedilir() {
        ProdGuvenlikKontrolu kontrol = olustur(true);
        ReflectionTestUtils.setField(kontrol, "corsAllowedOrigins", "");
        assertThrows(IllegalStateException.class, kontrol::kontrol);
    }

    @Test
    void strictDomainsLocalhostReddedilir() {
        ProdGuvenlikKontrolu kontrol = olustur(true);
        ReflectionTestUtils.setField(kontrol, "strictDomains", true);
        ReflectionTestUtils.setField(kontrol, "corsAllowedOrigins", "https://localhost");
        assertThrows(IllegalStateException.class, kontrol::kontrol);
    }

    @Test
    void strictDomainsGercekAlanAdiKabulEdilir() {
        ProdGuvenlikKontrolu kontrol = olustur(true);
        ReflectionTestUtils.setField(kontrol, "strictDomains", true);
        assertDoesNotThrow(kontrol::kontrol);
    }
}
