package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BarkodServiceTest {

    private final BarkodService service = new BarkodService();

    @Test
    void barkodPng_gecerliPngUretir() {
        byte[] png = service.barkodPng("FTR-2026-0001", 300, 80);

        assertNotNull(png);
        assertTrue(png.length > 0);
        // PNG imzasi: 89 50 4E 47
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 0x50, png[1]);
        assertEquals((byte) 0x4E, png[2]);
        assertEquals((byte) 0x47, png[3]);
    }

    @Test
    void barkodPng_bosIcerikteHataFirlatir() {
        // CODE128 bos icerikle kodlanamaz -> kontrollu RuntimeException
        assertThrows(RuntimeException.class, () -> service.barkodPng("", 200, 50));
    }
}
