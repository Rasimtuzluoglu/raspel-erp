package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Redis yokken (ObjectProvider bos) yerel fallback davranisi test edilir.
 */
class IdempotencyServiceTest {

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        @SuppressWarnings("unchecked")
        ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        service = new IdempotencyService(provider);
    }

    @Test
    void ilkIstekKilidiAlirIkinciAlamaz() {
        String anahtar = "idem:test:1";
        assertTrue(service.deneKilit(anahtar));
        assertFalse(service.deneKilit(anahtar));
    }

    @Test
    void tamamlananIslemSonucuDoner() {
        String anahtar = "idem:test:2";
        assertTrue(service.deneKilit(anahtar));
        service.tamamla(anahtar, 42L);

        Optional<Long> sonuc = service.tamamlananSonuc(anahtar);
        assertTrue(sonuc.isPresent());
        assertEquals(42L, sonuc.get());
        // Tamamlanmis anahtar yeniden rezerve edilemez.
        assertFalse(service.deneKilit(anahtar));
    }

    @Test
    void serbestBirakilanKilitYenidenAlinabilir() {
        String anahtar = "idem:test:3";
        assertTrue(service.deneKilit(anahtar));
        service.serbestBirak(anahtar);
        assertTrue(service.deneKilit(anahtar));
    }

    @Test
    void tamamlanmamisIstekIcinSonucYok() {
        String anahtar = "idem:test:4";
        assertTrue(service.deneKilit(anahtar));
        assertTrue(service.tamamlananSonuc(anahtar).isEmpty());
    }
}
