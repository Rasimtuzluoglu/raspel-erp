package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.finans.DovizKuru;
import com.raspel.erp.repository.finans.DovizKuruRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TcmbKurServiceTest {

    @Mock
    private DovizKuruRepository dovizKuruRepository;

    private TcmbKurService tcmbKurService;

    @BeforeEach
    void setUp() {
        tcmbKurService = new TcmbKurService(dovizKuruRepository);
    }

    private DovizKuru kur(String kod, String satis) {
        return DovizKuru.builder()
                .dovizKodu(kod)
                .satisKuru(new BigDecimal(satis))
                .tarih(LocalDate.now())
                .build();
    }

    @Test
    void cevir_ayniParaBirimiAynenDoner() {
        BigDecimal sonuc = tcmbKurService.cevir(new BigDecimal("100"), "USD", "usd");
        assertEquals(0, new BigDecimal("100").compareTo(sonuc));
    }

    @Test
    void cevir_sifirTutarSifirDoner() {
        assertEquals(BigDecimal.ZERO, tcmbKurService.cevir(BigDecimal.ZERO, "USD", "TRY"));
        assertEquals(BigDecimal.ZERO, tcmbKurService.cevir(null, "USD", "TRY"));
    }

    @Test
    void cevir_usdToTry() {
        when(dovizKuruRepository.findAll()).thenReturn(List.of(kur("USD", "34.50")));
        BigDecimal sonuc = tcmbKurService.cevir(new BigDecimal("100"), "USD", "TRY");
        assertEquals(0, new BigDecimal("3450.0000").compareTo(sonuc));
    }

    @Test
    void cevir_tryToUsd() {
        when(dovizKuruRepository.findAll()).thenReturn(List.of(kur("USD", "34.50")));
        BigDecimal sonuc = tcmbKurService.cevir(new BigDecimal("3450"), "TRY", "USD");
        assertEquals(0, new BigDecimal("100.0000").compareTo(sonuc));
    }

    @Test
    void cevir_eurToUsd() {
        when(dovizKuruRepository.findAll()).thenReturn(List.of(
                kur("USD", "34.50"),
                kur("EUR", "38.00")
        ));
        BigDecimal sonuc = tcmbKurService.cevir(new BigDecimal("100"), "EUR", "USD");
        assertEquals(0, new BigDecimal("110.1449").compareTo(sonuc));
    }

    @Test
    void cevir_bilinmeyenKodBireDoner() {
        when(dovizKuruRepository.findAll()).thenReturn(List.of(kur("USD", "34.50")));
        BigDecimal sonuc = tcmbKurService.cevir(new BigDecimal("100"), "XXX", "TRY");
        assertEquals(0, new BigDecimal("100.0000").compareTo(sonuc));
    }

    @Test
    void parseBtcFiyat_binanceYanitiniCozumler() {
        String json = "{\"symbol\":\"BTCTRY\",\"price\":\"2100000.50\"}";
        BigDecimal fiyat = ReflectionTestUtils.invokeMethod(tcmbKurService, "parseBtcFiyat", json);
        assertNotNull(fiyat);
        assertEquals(0, new BigDecimal("2100000.50").compareTo(fiyat));
    }

    @Test
    void parseBtcFiyat_coingeckoYanitiniCozumler() {
        String json = "{\"bitcoin\":{\"try\":2150000}}";
        BigDecimal fiyat = ReflectionTestUtils.invokeMethod(tcmbKurService, "parseBtcFiyat", json);
        assertNotNull(fiyat);
        assertEquals(0, new BigDecimal("2150000").compareTo(fiyat));
    }

    @Test
    void parseBtcFiyat_gecersizYanittaNullDoner() {
        assertNull(ReflectionTestUtils.invokeMethod(tcmbKurService, "parseBtcFiyat", "{}"));
    }
}
