package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelemetriHizSiniriTest {

    private long simdi = 1_000_000L;
    private final TelemetriHizSiniri sinir = new TelemetriHizSiniri(3, 1_000L, () -> simdi);

    @Test
    void limitAltindaIzinVerir() {
        assertTrue(sinir.izinVar(1L));
        assertTrue(sinir.izinVar(1L));
        assertTrue(sinir.izinVar(1L));
    }

    @Test
    void limitAsilincaReddeder() {
        sinir.izinVar(1L);
        sinir.izinVar(1L);
        sinir.izinVar(1L);

        assertFalse(sinir.izinVar(1L));
        assertFalse(sinir.izinVar(1L));
    }

    @Test
    void pencereGecinceYenidenIzinVerir() {
        sinir.izinVar(1L);
        sinir.izinVar(1L);
        sinir.izinVar(1L);
        assertFalse(sinir.izinVar(1L));

        simdi += 1_001L;

        assertTrue(sinir.izinVar(1L));
    }

    @Test
    void kullaniciYoksaReddeder() {
        assertFalse(sinir.izinVar(null));
    }

    @Test
    void kullanicilarBirbiriniEtkilemez() {
        sinir.izinVar(1L);
        sinir.izinVar(1L);
        sinir.izinVar(1L);
        assertFalse(sinir.izinVar(1L));

        assertTrue(sinir.izinVar(2L));
    }
}
