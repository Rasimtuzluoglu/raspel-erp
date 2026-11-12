package com.raspel.erp.service.sistem;

import com.raspel.erp.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Faz 8/9: sohbet/AI uclari icin hiz siniri (Redis + bellek ici yedek).
 */
class SohbetHizSiniriTest {

    @Test
    void pencereDolmadanSinirAsilmaz() {
        AtomicLong saat = new AtomicLong(1_000L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(null, saat::get);

        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> sinir.kontrolEt("k", 3, 1_000, "limit"));
            saat.addAndGet(100);
        }
    }

    @Test
    void pencereIcerisindeFazlaIstekReddedilir() {
        AtomicLong saat = new AtomicLong(1_000L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(null, saat::get);

        assertDoesNotThrow(() -> sinir.kontrolEt("k", 2, 1_000, "limit"));
        assertDoesNotThrow(() -> sinir.kontrolEt("k", 2, 1_000, "limit"));
        assertThrows(BusinessException.class, () -> sinir.kontrolEt("k", 2, 1_000, "limit"));
    }

    @Test
    void pencereGecinceTekrarIzinVerilir() {
        AtomicLong saat = new AtomicLong(1_000L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(null, saat::get);

        sinir.kontrolEt("k", 1, 1_000, "limit");
        saat.addAndGet(1_100);
        assertDoesNotThrow(() -> sinir.kontrolEt("k", 1, 1_000, "limit"));
    }

    @Test
    void anahtarlarBirbiriniEtkilemez() {
        AtomicLong saat = new AtomicLong(1_000L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(null, saat::get);

        sinir.kontrolEt("a", 1, 1_000, "limit");
        assertThrows(BusinessException.class, () -> sinir.kontrolEt("a", 1, 1_000, "limit"));
        assertDoesNotThrow(() -> sinir.kontrolEt("b", 1, 1_000, "limit"));
    }

    // ------------------------------------------------------------- Redis yolu

    @SuppressWarnings("unchecked")
    private ZSetOperations<String, String> zset(StringRedisTemplate redis) {
        ZSetOperations<String, String> z = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(z);
        return z;
    }

    @Test
    void redisVarsaOrtakSayacKullanilir() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> z = zset(redis);
        when(z.zCard("sohbet:hiz:k")).thenReturn(0L);
        AtomicLong saat = new AtomicLong(1_000L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(redis, saat::get);

        sinir.kontrolEt("k", 2, 1_000, "limit");

        verify(z).removeRangeByScore("sohbet:hiz:k", 0.0, 0.0);
        verify(z).add(eq("sohbet:hiz:k"), anyString(), eq(1000.0));
        verify(redis).expire("sohbet:hiz:k", Duration.ofMillis(1_000));
    }

    @Test
    void redisLimitiDoluysaReddeder() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> z = zset(redis);
        when(z.zCard("sohbet:hiz:k")).thenReturn(2L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(redis, () -> 1_000L);

        assertThrows(BusinessException.class, () -> sinir.kontrolEt("k", 2, 1_000, "limit"));
        verify(z, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void redisHataVerirseBellekIciYedegeDuser() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.opsForZSet()).thenThrow(new RuntimeException("redis down"));
        AtomicLong saat = new AtomicLong(1_000L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(redis, saat::get);

        assertDoesNotThrow(() -> sinir.kontrolEt("k", 1, 1_000, "limit"));
        assertThrows(BusinessException.class, () -> sinir.kontrolEt("k", 1, 1_000, "limit"));
    }
}
