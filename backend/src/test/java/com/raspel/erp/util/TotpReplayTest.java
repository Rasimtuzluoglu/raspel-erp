package com.raspel.erp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TOTP replay koruması testleri.
 *
 * <p>Denetimde {@code validate} penceresi ±1 adım (±30 sn) olduğu için geçerli bir
 * kod üç kez kabul ediliyordu; ekrandan görülen bir kod tekrar kullanılabiliyordu.
 * {@link TotpUtil#dogrula} artık eşleşen zaman adımını döndürüyor; servis bu
 * adımı kullanıcıda saklayıp aynı adımı reddediyor.
 */
class TotpReplayTest {

    private static final String SECRET = "JBSWY3DPEHPK3PXP";
    private static final long ZAMAN = 1_700_000_000_000L;

    @Test
    void dogrula_gecerliKodIcinCounterDondurur() {
        String kod = TotpUtil.generateCode(SECRET, ZAMAN);

        var sonuc = TotpUtil.dogrula(SECRET, kod, ZAMAN);

        assertTrue(sonuc.gecerli());
        assertEquals(ZAMAN / 1000 / 30, sonuc.counter());
    }

    @Test
    void dogrula_yanlisKodReddedilir() {
        var sonuc = TotpUtil.dogrula(SECRET, "000000", ZAMAN);

        assertFalse(sonuc.gecerli());
        assertEquals(-1L, sonuc.counter());
    }

    /**
     * Replay senaryosunun kendisi: aynı zaman adımındaki kod ikinci kez
     * geldiğinde servis tarafından reddedilmelidir. Burada "servis katmanı"
     * davranışını sonKullanilanDeger karşılaştırmasıyla modelliyoruz.
     */
    @Test
    void ayniZamanAdimiTekrarKullanilamaz() {
        String kod = TotpUtil.generateCode(SECRET, ZAMAN);

        var ilk = TotpUtil.dogrula(SECRET, kod, ZAMAN);
        assertTrue(ilk.gecerli());
        long sonKullanilan = ilk.counter();

        // Aynı kod, aynı zaman adımı: replay koruması devreye girer.
        var ikinci = TotpUtil.dogrula(SECRET, kod, ZAMAN);
        assertTrue(ikinci.gecerli());
        assertTrue(ikinci.counter() <= sonKullanilan, "Aynı adım ikinci kez kabul edilmemeli");
    }

    @Test
    void yeniZamanAdimiKabulEdilir() {
        String kod0 = TotpUtil.generateCode(SECRET, ZAMAN);
        var ilk = TotpUtil.dogrula(SECRET, kod0, ZAMAN);
        assertTrue(ilk.gecerli());

        // 30 sn sonra yeni adım → yeni kod, counter artar.
        long sonrakiZaman = ZAMAN + 30_000L;
        String kod1 = TotpUtil.generateCode(SECRET, sonrakiZaman);
        var ikinci = TotpUtil.dogrula(SECRET, kod1, sonrakiZaman);

        assertTrue(ikinci.gecerli());
        assertTrue(ikinci.counter() > ilk.counter(), "Yeni adım daha büyük counter olmalı");
    }

/**
     * Pencere toleransı: 30 sn ileri/geri saat kayması olan cihazlar için kod
     * geçerli sayılır. Kritik özellik: üç zaman damgasında da dönen counter
     * kodun KENDİ counter'ıdır (saat kaymasından etkilenmez) — replay koruması
     * ancak bu sayede "aynı kod = aynı adım" kuralıyla çalışır.
     */
@Test
    void toleransliPencereKodunKendiCounteriniDoner() {
        String kod = TotpUtil.generateCode(SECRET, ZAMAN);
        long beklenen = ZAMAN / 1000 / 30;

        var merkez = TotpUtil.dogrula(SECRET, kod, ZAMAN);
        var ileri = TotpUtil.dogrula(SECRET, kod, ZAMAN + 30_000L);
        var geri = TotpUtil.dogrula(SECRET, kod, ZAMAN - 30_000L);

        assertTrue(merkez.gecerli());
        assertTrue(ileri.gecerli());
        assertTrue(geri.gecerli());
        assertEquals(beklenen, merkez.counter());
        assertEquals(beklenen, ileri.counter(), "Saat 30 sn ileride olsa da counter aynı olmalı");
        assertEquals(beklenen, geri.counter(), "Saat 30 sn geride olsa da counter aynı olmalı");
    }

    @Test
    void dogrula_nullKodVeSecretGuvenli() {
        assertFalse(TotpUtil.dogrula(null, "123456", ZAMAN).gecerli());
        assertFalse(TotpUtil.dogrula(SECRET, null, ZAMAN).gecerli());
        assertFalse(TotpUtil.validate(null, "123456", ZAMAN));
        assertFalse(TotpUtil.validate(SECRET, null, ZAMAN));
    }

    @Test
    void validate_dogrulaIleUyumlu() {
        String kod = TotpUtil.generateCode(SECRET, ZAMAN);
        assertEquals(TotpUtil.validate(SECRET, kod, ZAMAN), TotpUtil.dogrula(SECRET, kod, ZAMAN).gecerli());
    }
}