package com.raspel.erp.service.sistem;

import com.raspel.erp.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Faz 8: sohbet/AI uclari icin kayan pencere hiz siniri.
 */
class SohbetHizSiniriTest {

    @Test
    void pencereDolmadanSinirAsilmaz() {
        AtomicLong saat = new AtomicLong(1_000L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(saat::get);

        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> sinir.kontrolEt("k", 3, 1_000, "limit"));
            saat.addAndGet(100);
        }
    }

    @Test
    void pencereIcerisindeFazlaIstekReddedilir() {
        AtomicLong saat = new AtomicLong(1_000L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(saat::get);

        assertDoesNotThrow(() -> sinir.kontrolEt("k", 2, 1_000, "limit"));
        assertDoesNotThrow(() -> sinir.kontrolEt("k", 2, 1_000, "limit"));
        assertThrows(BusinessException.class, () -> sinir.kontrolEt("k", 2, 1_000, "limit"));
    }

    @Test
    void pencereGecinceTekrarIzinVerilir() {
        AtomicLong saat = new AtomicLong(1_000L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(saat::get);

        sinir.kontrolEt("k", 1, 1_000, "limit");
        saat.addAndGet(1_100);
        assertDoesNotThrow(() -> sinir.kontrolEt("k", 1, 1_000, "limit"));
    }

    @Test
    void anahtarlarBirbiriniEtkilemez() {
        AtomicLong saat = new AtomicLong(1_000L);
        SohbetHizSiniri sinir = new SohbetHizSiniri(saat::get);

        sinir.kontrolEt("a", 1, 1_000, "limit");
        assertThrows(BusinessException.class, () -> sinir.kontrolEt("a", 1, 1_000, "limit"));
        assertDoesNotThrow(() -> sinir.kontrolEt("b", 1, 1_000, "limit"));
    }
}
