package com.raspel.erp.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FaturaTutarTest {

    private static final BigDecimal KDVO = new BigDecimal("20");

    @Test
    void satir_kdvDahilFiyattanMatrahiAyristirir() {
        // 120 (KDV dahil) -> net 100, kdv 20, brut 120
        FaturaTutar.Satir s = FaturaTutar.satir(new BigDecimal("120"), BigDecimal.ONE, BigDecimal.ZERO, KDVO);
        assertEquals(0, s.net().compareTo(new BigDecimal("100")));
        assertEquals(0, s.kdv().compareTo(new BigDecimal("20")));
        assertEquals(0, s.brut().compareTo(new BigDecimal("120")));
    }

    @Test
    void satir_iskontoSonrasiKdvDahilBruttenAyristirir() {
        // 1000 x2 = 2000; %10 iskonto -> 1800; net 1500, kdv 300
        FaturaTutar.Satir s = FaturaTutar.satir(new BigDecimal("1000"), new BigDecimal("2"),
                new BigDecimal("10"), KDVO);
        assertEquals(0, s.brut().compareTo(new BigDecimal("1800")));
        assertEquals(0, s.net().compareTo(new BigDecimal("1500")));
        assertEquals(0, s.kdv().compareTo(new BigDecimal("300")));
    }

    @Test
    void satir_sifirKdvOranindaNetBruteEsit() {
        FaturaTutar.Satir s = FaturaTutar.satir(new BigDecimal("250"), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(0, s.net().compareTo(new BigDecimal("250")));
        assertEquals(0, s.kdv().compareTo(BigDecimal.ZERO));
    }

    @Test
    void belge_araToplamArtıKdvGenelToplamaEsit() {
        List<FaturaTutar.Satir> satirlar = List.of(
                FaturaTutar.satir(new BigDecimal("1200"), BigDecimal.ONE, BigDecimal.ZERO, KDVO),
                FaturaTutar.satir(new BigDecimal("600"), BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("10")));
        FaturaTutar.Belge b = FaturaTutar.belge(satirlar, BigDecimal.ZERO);
        assertEquals(0, b.araToplam().add(b.kdv()).compareTo(b.genelToplam()));
        assertEquals(0, b.genelToplam().compareTo(b.brutToplam()));
    }

    @Test
    void belge_genelIskontoGenelToplamiDuserVeDegismeziKorur() {
        List<FaturaTutar.Satir> satirlar = List.of(
                FaturaTutar.satir(new BigDecimal("1000"), new BigDecimal("2"), BigDecimal.ZERO, KDVO));
        // brutToplam 2000, genel iskonto 200 -> genelToplam 1800
        FaturaTutar.Belge b = FaturaTutar.belge(satirlar, new BigDecimal("200"));
        assertEquals(0, b.genelToplam().compareTo(new BigDecimal("1800")));
        assertEquals(0, b.araToplam().add(b.kdv()).compareTo(b.genelToplam()));
    }

    @Test
    void belge_bosKalemlerdeSifirDoner() {
        FaturaTutar.Belge b = FaturaTutar.belge(List.of(), BigDecimal.TEN);
        assertEquals(0, b.genelToplam().compareTo(BigDecimal.ZERO));
        assertEquals(0, b.araToplam().compareTo(BigDecimal.ZERO));
        assertEquals(0, b.kdv().compareTo(BigDecimal.ZERO));
    }

    @Test
    void belge_genelIskontoBrutToplamiAsamaz() {
        List<FaturaTutar.Satir> satirlar = List.of(
                FaturaTutar.satir(new BigDecimal("100"), BigDecimal.ONE, BigDecimal.ZERO, KDVO));
        FaturaTutar.Belge b = FaturaTutar.belge(satirlar, new BigDecimal("9999"));
        assertEquals(0, b.genelToplam().compareTo(BigDecimal.ZERO));
    }
}
