package com.raspel.erp.service.sistem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PdfMetinTest {

    @Test
    void varsayilanDilTurkce() {
        PdfMetin m = PdfMetin.tr();
        assertFalse(m.ingilizce());
        assertEquals("SATIŞ FATURASI", m.t("satisFaturasi"));
        assertEquals("MÜŞTERİ", m.t("musteri"));
        assertEquals("GENEL TOPLAM", m.t("genelToplam"));
    }

    @Test
    void ingilizceDilSecilir() {
        PdfMetin m = PdfMetin.of("en");
        assertTrue(m.ingilizce());
        assertEquals("SALES INVOICE", m.t("satisFaturasi"));
        assertEquals("CUSTOMER", m.t("musteri"));
        assertEquals("GRAND TOTAL", m.t("genelToplam"));
    }

    @Test
    void acceptLanguageEnUsIngilizceyeCozulur() {
        PdfMetin m = PdfMetin.of("en-US,en;q=0.9");
        assertTrue(m.ingilizce());
    }

    @Test
    void acceptLanguageTrTurkceKalir() {
        PdfMetin m = PdfMetin.of("tr-TR,tr;q=0.9");
        assertFalse(m.ingilizce());
    }

    @Test
    void bilinmeyenDilTurkceyeDuser() {
        PdfMetin m = PdfMetin.of("de");
        assertFalse(m.ingilizce());
        assertEquals("SATIŞ FATURASI", m.t("satisFaturasi"));
    }

    @Test
    void nullDilTurkceVarsayilan() {
        assertFalse(PdfMetin.of(null).ingilizce());
    }

    @Test
    void tanimsizAnahtarAnahtarAdiniDoner() {
        assertEquals("bilinmeyenAnahtar", PdfMetin.tr().t("bilinmeyenAnahtar"));
    }

    @Test
    void tumEtiketlerIkiDildeDeDolu() {
        // Ayni anahtar kumesi her iki dilde de tanimli olmali (bos deger yok).
        String[] anahtarlar = {
                "satisFaturasi", "alisFaturasi", "musteri", "tedarikci", "belgeBilgileri",
                "faturaNo", "faturaTarihi", "vadeTarihi", "durum", "paraBirimi", "olusturan",
                "sira", "urunHizmet", "miktar", "birimFiyat", "kdvYuzde", "tutar",
                "kalemBulunmuyor", "araToplam", "iskonto", "kdv", "genelToplam",
                "odenen", "kalan", "odemeDurumu", "toplamAgirlik", "imzaKase", "imzaKaseIrsaliye",
                "teslimAlanImzasi", "siparisFormu", "siparisBilgileri", "siparisNo", "tarih",
                "aciklama", "birim", "irsaliye", "irsaliyeBilgileri", "irsaliyeNo",
                "teslimatFisi", "teslimatBilgileri", "teslimatAdresi", "teslimEden",
                "teslimAlan", "teslimTarihi", "fatura", "bagliKalemYok", "rafEtiketi", "fiyat"
        };
        PdfMetin tr = PdfMetin.tr();
        PdfMetin en = PdfMetin.of("en");
        for (String a : anahtarlar) {
            assertNotEquals(a, tr.t(a), "TR eksik: " + a);
            assertNotEquals(a, en.t(a), "EN eksik: " + a);
        }
    }
}
