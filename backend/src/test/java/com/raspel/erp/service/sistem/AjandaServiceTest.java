package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.AjandaOlayDTO;
import com.raspel.erp.entity.sistem.Gorev;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.sistem.GorevRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AjandaServiceTest {

    @Mock
    private GorevRepository gorevRepository;
    @Mock
    private FaturaRepository faturaRepository;
    @Mock
    private com.raspel.erp.repository.sistem.AjandaGorevRepository ajandaGorevRepository;
    @Mock
    private com.raspel.erp.repository.sistem.AjandaHatirlaticiRepository ajandaHatirlaticiRepository;

    @InjectMocks
    private AjandaService ajandaService;

    @Test
    void olaylar_gorevleriVeVadeleriBirlesitiripTariheGoreSiralar() {
        LocalDate bas = LocalDate.of(2026, 8, 1);
        LocalDate bit = LocalDate.of(2026, 8, 31);

        Gorev g1 = Gorev.builder().id(1L).ad("Rapor hazırla").durum("YAPILACAK").atanan("Ali").baslangic(LocalDate.of(2026, 8, 10)).build();
        Gorev g2 = Gorev.builder().id(2L).ad("Stok sayımı").durum("TAMAMLANDI").baslangic(LocalDate.of(2026, 8, 3)).build();
        when(gorevRepository.sirketGorevleri(1L, bas, bit)).thenReturn(List.of(g1, g2));

        Fatura f1 = new Fatura();
        f1.setFaturaNumarasi("FTR-1-2026-000001");
        f1.setVadeTarihi(LocalDate.of(2026, 8, 5));
        f1.setKalanTutar(new java.math.BigDecimal("1500.00"));
        when(faturaRepository.findVadesiYaklasan(eq(1L), eq(Fatura.FaturaDurum.KESILDI), any(), eq(bas), eq(bit), any()))
                .thenReturn(List.of(f1));

        List<AjandaOlayDTO> olaylar = ajandaService.olaylar(1L, bas, bit, null);

        assertEquals(3, olaylar.size());
        assertEquals("GOREV", olaylar.get(0).getTip());
        assertEquals("Stok sayımı", olaylar.get(0).getBaslik());
        assertEquals("VADE", olaylar.get(1).getTip());
        assertEquals("GOREV", olaylar.get(2).getTip());
        assertTrue(olaylar.get(0).getTarih().isBefore(olaylar.get(2).getTarih()));
        // Çapraz gezinme için kaynak bilgisi doldurulmalı
        assertEquals("FATURA", olaylar.get(1).getKaynakTip());
        assertEquals("FTR-1-2026-000001", olaylar.get(1).getKaynakNo());
        assertEquals("PROJE_GOREVI", olaylar.get(0).getKaynakTip());
        assertEquals(2L, olaylar.get(0).getKaynakId());
    }

    @Test
    void yaklasan_bugundenItibarenOlaylariDoner() {
        LocalDate bugun = LocalDate.now();
        when(gorevRepository.sirketGorevleri(1L, bugun, bugun.plusDays(10))).thenReturn(List.of());
        when(faturaRepository.findVadesiYaklasan(eq(1L), any(), any(), eq(bugun), eq(bugun.plusDays(10)), any()))
                .thenReturn(List.of());

        List<AjandaOlayDTO> olaylar = ajandaService.yaklasan(1L, null, 10);

        assertTrue(olaylar.isEmpty());
        org.mockito.Mockito.verify(gorevRepository).sirketGorevleri(1L, bugun, bugun.plusDays(10));
    }

    @Test
    void olaylar_bosSonucBosListeDoner() {
        LocalDate bas = LocalDate.of(2026, 8, 1);
        LocalDate bit = LocalDate.of(2026, 8, 31);
        when(gorevRepository.sirketGorevleri(1L, bas, bit)).thenReturn(List.of());
        when(faturaRepository.findVadesiYaklasan(eq(1L), any(), any(), eq(bas), eq(bit), any())).thenReturn(List.of());

        List<AjandaOlayDTO> olaylar = ajandaService.olaylar(1L, bas, bit, null);

        assertTrue(olaylar.isEmpty());
    }

    @Test
    void olaylar_gorevBaslangicNullIseBaslangicTarihiniKullanir() {
        LocalDate bas = LocalDate.of(2026, 8, 1);
        LocalDate bit = LocalDate.of(2026, 8, 31);
        Gorev g = Gorev.builder().id(1L).ad("Görev").durum("YAPILACAK").build();
        when(gorevRepository.sirketGorevleri(1L, bas, bit)).thenReturn(List.of(g));
        when(faturaRepository.findVadesiYaklasan(eq(1L), any(), any(), eq(bas), eq(bit), any())).thenReturn(List.of());

        List<AjandaOlayDTO> olaylar = ajandaService.olaylar(1L, bas, bit, null);

        assertEquals(1, olaylar.size());
        assertEquals(bas, olaylar.get(0).getTarih());
    }

    @Test
    void ozet_kpiDegerleriniDoner() {
        LocalDate bugun = LocalDate.now();
        when(gorevRepository.sirketGorevleri(1L, bugun, bugun)).thenReturn(List.of());
        when(faturaRepository.findVadesiYaklasan(eq(1L), any(), any(), eq(bugun), eq(bugun), any()))
                .thenReturn(List.of());
        when(ajandaGorevRepository.findByKullaniciIdAndBitisTarihiBetweenOrderByBitisTarihiAsc(7L, bugun, bugun))
                .thenReturn(List.of());
        when(ajandaHatirlaticiRepository.findByKullaniciIdOrderByHatirlatmaZamaniAsc(7L))
                .thenReturn(List.of());
        when(faturaRepository.toplamKalanVadeAraliginda(eq(1L), any(), any(), any(), any()))
                .thenReturn(new java.math.BigDecimal("2500.00"));
        when(ajandaGorevRepository.countByKullaniciIdAndDurum(7L, "BEKLIYOR")).thenReturn(4L);
        when(ajandaGorevRepository.countByKullaniciIdAndDurumAndBitisTarihiBefore(eq(7L), eq("BEKLIYOR"), any()))
                .thenReturn(2L);

        var ozet = ajandaService.ozet(1L, 7L);

        assertEquals(0L, ozet.getBugunOlay());
        assertEquals(new java.math.BigDecimal("2500.00"), ozet.getBuAyVadeToplam());
        assertEquals(4L, ozet.getBekleyenGorev());
        assertEquals(2L, ozet.getGecikmisGorev());
    }

    @Test
    void ozet_sirketYoksaSifir() {
        var ozet = ajandaService.ozet(null, 7L);
        assertEquals(0L, ozet.getBugunOlay());
        assertEquals(java.math.BigDecimal.ZERO, ozet.getBuAyVadeToplam());
        assertEquals(0L, ozet.getBekleyenGorev());
        assertEquals(0L, ozet.getGecikmisGorev());
    }
}
