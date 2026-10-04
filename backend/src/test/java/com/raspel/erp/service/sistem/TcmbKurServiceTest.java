package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.finans.DovizKuru;
import com.raspel.erp.exception.BusinessException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TcmbKurServiceTest {

    @Mock
    private DovizKuruRepository dovizKuruRepository;

    private TcmbKurService tcmbKurService;

    @BeforeEach
    void setUp() {
        // Spy: `tcmbKurlariniGuncelle` gercek ag cagrisi yapmasin diye boslanir.
        tcmbKurService = spy(new TcmbKurService(dovizKuruRepository));
        lenient().doNothing().when(tcmbKurService).tcmbKurlariniGuncelle();
        // cevir_* testleri findAll üzerinden çalışır; bugünün kuru var sayılır (ağ çağrısı tetiklenmez)
        lenient().when(dovizKuruRepository.countByTarih(any())).thenReturn(1L);
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
        when(dovizKuruRepository.findFirstByDovizKoduOrderByTarihDesc("USD"))
                .thenReturn(java.util.Optional.of(kur("USD", "34.50")));
        BigDecimal sonuc = tcmbKurService.cevir(new BigDecimal("100"), "USD", "TRY");
        assertEquals(0, new BigDecimal("3450.0000").compareTo(sonuc));
    }

    @Test
    void cevir_tryToUsd() {
        when(dovizKuruRepository.findFirstByDovizKoduOrderByTarihDesc("USD"))
                .thenReturn(java.util.Optional.of(kur("USD", "34.50")));
        BigDecimal sonuc = tcmbKurService.cevir(new BigDecimal("3450"), "TRY", "USD");
        assertEquals(0, new BigDecimal("100.0000").compareTo(sonuc));
        // TRY taban birim: yenileme tetiklenmemeli.
        verify(tcmbKurService, never()).tcmbKurlariniGuncelle();
    }

    @Test
    void cevir_eurToUsd() {
        when(dovizKuruRepository.findFirstByDovizKoduOrderByTarihDesc("EUR"))
                .thenReturn(java.util.Optional.of(kur("EUR", "38.00")));
        when(dovizKuruRepository.findFirstByDovizKoduOrderByTarihDesc("USD"))
                .thenReturn(java.util.Optional.of(kur("USD", "34.50")));
        BigDecimal sonuc = tcmbKurService.cevir(new BigDecimal("100"), "EUR", "USD");
        assertEquals(0, new BigDecimal("110.1449").compareTo(sonuc));
    }

    /**
     * C2 (fail-closed): bilinmeyen/kursuz para birimi artık 1:1 kabul edilmiyor.
     * Önceden sessizce kur=1 dönüyordu; "100 USD" yerine 100 TRY muhasebeye
     * yazılıyordu. Artık işlem durdurulur.
     */
    @Test
    void cevir_bilinmeyenKodHataVerir() {
        when(dovizKuruRepository.findFirstByDovizKoduOrderByTarihDesc("XXX"))
                .thenReturn(java.util.Optional.empty());
        var hata = assertThrows(BusinessException.class,
                () -> tcmbKurService.cevir(new BigDecimal("100"), "XXX", "TRY"));
        assertTrue(hata.getMessage().contains("XXX"));
        // Kodsuz kod tespit edilince bir kez yenileme denenir, sonra hata verilir.
        verify(tcmbKurService, times(1)).tcmbKurlariniGuncelle();
    }

    /** C2: TRY -> TRY kur sorgusuz 1:1 geçer (kayıt aranmaz). */
    @Test
    void cevir_tryToTryKurSorgulanmaz() {
        BigDecimal sonuc = tcmbKurService.cevir(new BigDecimal("100"), "TRY", "TRY");
        assertEquals(0, new BigDecimal("100").compareTo(sonuc));
        verifyNoInteractions(dovizKuruRepository);
    }

    /** C2: ayni para birimi 1:1 gecer; kur tablosu/yenileme hic devreye girmez. */
    @Test
    void cevir_ayniKodKurTablosuBoskenDahilCalisir() {
        BigDecimal sonuc = tcmbKurService.cevir(new BigDecimal("100"), "USD", "USD");
        assertEquals(0, new BigDecimal("100").compareTo(sonuc));
        verifyNoInteractions(dovizKuruRepository);
        verify(tcmbKurService, never()).tcmbKurlariniGuncelle();
    }

    /** C2: hedef kod kurali yoksa da sessizce 1:1 olmaz. */
    @Test
    void cevir_hedefKoduKursuzHataVerir() {
        when(dovizKuruRepository.findFirstByDovizKoduOrderByTarihDesc("USD"))
                .thenReturn(java.util.Optional.of(kur("USD", "34.50")));
        when(dovizKuruRepository.findFirstByDovizKoduOrderByTarihDesc("CHF"))
                .thenReturn(java.util.Optional.empty());
        var hata = assertThrows(BusinessException.class,
                () -> tcmbKurService.cevir(new BigDecimal("100"), "USD", "CHF"));
        assertTrue(hata.getMessage().contains("CHF"));
    }

    /** C2: kod normalizasyonu (bos -> hata). */
    @Test
    void cevir_bosKodHataVerir() {
        assertThrows(BusinessException.class,
                () -> tcmbKurService.cevir(new BigDecimal("100"), " ", "TRY"));
        assertThrows(BusinessException.class,
                () -> tcmbKurService.cevir(new BigDecimal("100"), "USD", null));
    }

    @Test
    void tumKurlariGetir_bugunKuruVarsaTekFindAllIleDoner() {
        when(dovizKuruRepository.findAll()).thenReturn(List.of(kur("USD", "34.50")));

        var liste = tcmbKurService.tumKurlariGetir();

        assertEquals(1, liste.size());
        // Güncel kullanılabilir durumdayken TCMB'ye ağ çağrısı yapılmaz, findAll tek kez çalışır
        verify(dovizKuruRepository, times(1)).findAll();
        verify(dovizKuruRepository, never()).save(any());
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
