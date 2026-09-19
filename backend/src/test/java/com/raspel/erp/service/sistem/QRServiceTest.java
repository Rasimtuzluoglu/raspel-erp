package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QRServiceTest {

    private final QRService service = new QRService();

    @Test
    void qrPng_gecerliPngUretir() {
        byte[] png = service.qrPng("https://raspel-erp.example/fatura/1", 200);

        assertNotNull(png);
        assertTrue(png.length > 0);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 0x50, png[1]);
        assertEquals((byte) 0x4E, png[2]);
        assertEquals((byte) 0x47, png[3]);
    }

    @Test
    void qrPng_sigdirilamayacakVerideHataFirlatir() {
        // QR kapasitesini asan icerik -> kontrollu RuntimeException
        assertThrows(RuntimeException.class, () -> service.qrPng("x".repeat(5000), 50));
    }
}
