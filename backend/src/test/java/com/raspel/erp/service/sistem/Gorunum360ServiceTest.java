package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import com.raspel.erp.service.envanter.MaliyetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Gorunum360ServiceTest {

    @Mock private FaturaRepository faturaRepository;
    @Mock private StokRepository stokRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private TeslimatRepository teslimatRepository;
    @Mock private MaliyetService maliyetService;
    @InjectMocks private Gorunum360Service service;

    @Test
    void calisanPerformans_teslimatSayilariniGruplar() {
        Teslimat t1 = Teslimat.builder().id(1L).sirketId(1L).driverId(10L).durum("TESLIM_EDILDI").teslimEdenAd("Şoför 1").build();
        Teslimat t2 = Teslimat.builder().id(2L).sirketId(1L).driverId(10L).durum("YOLDA").teslimEdenAd("Şoför 1").build();
        Teslimat t3 = Teslimat.builder().id(3L).sirketId(1L).driverId(20L).durum("TESLIM_EDILDI").teslimEdenAd("Şoför 2").build();
        when(teslimatRepository.findBySirketId(1L)).thenReturn(List.of(t1, t2, t3));

        var sonuc = service.calisanPerformans360(1L);

        assertEquals(2, sonuc.getCalisanlar().size());
        assertEquals(3, sonuc.getToplamTeslimat());
        assertEquals(2, sonuc.getTamamlananTeslimat());
        var sofor1 = sonuc.getCalisanlar().stream().filter(c -> c.getId().equals(10L)).findFirst().orElseThrow();
        assertEquals(2, sofor1.getToplamTeslimat());
        assertEquals(1, sofor1.getTamamlananTeslimat());
        assertEquals(1, sofor1.getBekleyenTeslimat());
    }

    @Test
    void musteriSegment_riskliNetBorcKurali() {
        // Riskli: net borç (negatif bakiye) kredi limitini aşmış.
        CariHesap vip = CariHesap.builder().id(1L).ad("VIP A.Ş.").bakiye(BigDecimal.ZERO).build();
        CariHesap riskli = CariHesap.builder().id(2L).ad("Riskli Ltd.")
                .bakiye(BigDecimal.valueOf(-25000)).krediLimiti(BigDecimal.valueOf(20000)).build();
        // Alacaklı (pozitif bakiye) ama ödenmemiş faturası var -> RISKLI olmamalı.
        CariHesap alacakli = CariHesap.builder().id(3L).ad("Alacaklı A.Ş.")
                .bakiye(BigDecimal.valueOf(8000)).build();
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(vip, riskli, alacakli));

        Fatura fVip = Fatura.builder().id(1L).tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(vip).genelToplam(BigDecimal.valueOf(150000)).kalanTutar(BigDecimal.ZERO)
                .tarih(LocalDate.now().minusDays(5)).build();
        Fatura fRisk = Fatura.builder().id(2L).tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(riskli).genelToplam(BigDecimal.valueOf(30000)).kalanTutar(BigDecimal.valueOf(15000))
                .tarih(LocalDate.now().minusDays(3)).build();
        Fatura fAlacak = Fatura.builder().id(3L).tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(alacakli).genelToplam(BigDecimal.valueOf(20000)).kalanTutar(BigDecimal.valueOf(15000))
                .tarih(LocalDate.now().minusDays(3)).build();
        when(faturaRepository.findBySirketIdAndTarihBetweenKalemli(eq(1L), any(), any()))
                .thenReturn(List.of(fVip, fRisk, fAlacak));

        var sonuc = service.musteriSegmentasyon(1L);

        var vipSatir = sonuc.getMusteriler().stream().filter(m -> m.getCariId().equals(1L)).findFirst().orElseThrow();
        var riskSatir = sonuc.getMusteriler().stream().filter(m -> m.getCariId().equals(2L)).findFirst().orElseThrow();
        var alacakSatir = sonuc.getMusteriler().stream().filter(m -> m.getCariId().equals(3L)).findFirst().orElseThrow();
        assertEquals("VIP", vipSatir.getSegment());
        assertEquals("RISKLI", riskSatir.getSegment());
        assertTrue(riskSatir.getGerekce().contains("Net borç"));
        assertNotEquals("RISKLI", alacakSatir.getSegment());
    }

    @Test
    void musteriSegment_limitAltindakiBorcRiskliDegil() {
        CariHesap borclu = CariHesap.builder().id(4L).ad("Küçük Borçlu")
                .bakiye(BigDecimal.valueOf(-3000)).build();
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(borclu));
        Fatura f = Fatura.builder().id(1L).tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(borclu).genelToplam(BigDecimal.valueOf(5000)).kalanTutar(BigDecimal.valueOf(3000))
                .tarih(LocalDate.now().minusDays(3)).build();
        when(faturaRepository.findBySirketIdAndTarihBetweenKalemli(eq(1L), any(), any())).thenReturn(List.of(f));

        var sonuc = service.musteriSegmentasyon(1L);

        assertNotEquals("RISKLI", sonuc.getMusteriler().get(0).getSegment());
    }

    @Test
    void musteriSegment_pasifKurali() {
        CariHesap eski = CariHesap.builder().id(5L).ad("Eski Müşteri").bakiye(BigDecimal.ZERO).build();
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(eski));
        Fatura f = Fatura.builder().id(1L).tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(eski).genelToplam(BigDecimal.valueOf(5000)).kalanTutar(BigDecimal.ZERO)
                .tarih(LocalDate.now().minusDays(200)).build();
        when(faturaRepository.findBySirketIdAndTarihBetweenKalemli(eq(1L), any(), any())).thenReturn(List.of(f));

        var sonuc = service.musteriSegmentasyon(1L);

        assertEquals("PASIF", sonuc.getMusteriler().get(0).getSegment());
    }

    @Test
    void stokKar_bosVeri() {
        when(faturaRepository.findBySirketIdAndTarihBetweenKalemli(eq(1L), any(), any())).thenReturn(List.of());
        var sonuc = service.stokKar360(1L, null, null);
        assertEquals(0, sonuc.getToplamUrun());
        assertEquals(0, sonuc.getToplamCiro().compareTo(BigDecimal.ZERO));
    }
}
