package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PdfMetinTest {

    @Test
    void varsayilanTurkce() {
        PdfMetin m = PdfMetin.of(null);
        assertEquals("tr", m.dil());
        assertEquals("SATIŞ FATURASI", m.t("satisFaturasi"));
        assertEquals("RAF ETİKETİ", m.t("rafEtiketi"));
        assertEquals("Kod", m.t("kod"));
    }

    @Test
    void ingilizceDesteklenir() {
        PdfMetin m = PdfMetin.of("en-US,en;q=0.9");
        assertEquals("en", m.dil());
        assertTrue(m.ingilizce());
        assertEquals("SALES INVOICE", m.t("satisFaturasi"));
        assertEquals("SHELF LABEL", m.t("rafEtiketi"));
        assertEquals("Code", m.t("kod"));
        assertEquals("ACCOUNT STATEMENT", m.t("cariEkstre"));
    }

    @Test
    void bilinmeyenDilVeAnahtarTrDavranisi() {
        PdfMetin m = PdfMetin.of("de");
        assertEquals("tr", m.dil());
        // tanımsız anahtar anahtar adına düşer
        assertEquals("olmayanAnahtar", m.t("olmayanAnahtar"));
    }
}
