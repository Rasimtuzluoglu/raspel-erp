package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.DashboardDTO;
import com.raspel.erp.dto.sistem.DashboardGorevDTO;
import com.raspel.erp.dto.sistem.DashboardGrafikDTO;
import com.raspel.erp.dto.sistem.DashboardKarAnaliziDTO;
import com.raspel.erp.dto.sistem.DashboardOzetDTO;
import com.raspel.erp.dto.sistem.KokpitDTO;
import com.raspel.erp.dto.sistem.NakitAkisiProjeksiyonDTO;
import com.raspel.erp.dto.sistem.RaporDTO;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.service.finans.CariHesapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KokpitServiceTest {

    @Mock private DashboardService dashboardService;
    @Mock private RaporService raporService;
    @Mock private CariHesapService cariHesapService;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private FaturaRepository faturaRepository;

    @InjectMocks private KokpitService service;

    @Test
    void kokpitTumBolumleriDoldurur() {
        stubDoluVeri();

        KokpitDTO kokpit = service.kokpitGetir(1L, "ADMIN", "BU_AY");

        assertNotNull(kokpit);
        assertEquals("BU_AY", kokpit.getDonem().getKod());

        List<String> anahtarlar = kokpit.getOzet().stream().map(KokpitDTO.KpiDTO::getAnahtar).toList();
        assertEquals(List.of("netNakit", "alacak", "verecek", "netCalismaSermayesi",
                "ciro", "brutKar", "marj", "vadesiGecen"), anahtarlar);
        assertEquals(8, kokpit.getOzet().size());

        assertEquals(0, para(kokpit, "netNakit").compareTo(new BigDecimal("12000")));
        assertEquals(0, para(kokpit, "alacak").compareTo(new BigDecimal("60000")));
        assertEquals(0, para(kokpit, "verecek").compareTo(new BigDecimal("20000")));
        assertEquals(0, para(kokpit, "netCalismaSermayesi").compareTo(new BigDecimal("52000")));
        assertEquals(0, para(kokpit, "ciro").compareTo(new BigDecimal("100000")));
        assertEquals(0, para(kokpit, "brutKar").compareTo(new BigDecimal("25000")));
        assertEquals(0, para(kokpit, "marj").compareTo(new BigDecimal("25.0")));
        assertEquals(0, para(kokpit, "vadesiGecen").compareTo(new BigDecimal("4000")));

        assertEquals(4, kokpit.getKritikDurum().size());
        assertTrue(kokpit.getKritikDurum().stream().anyMatch(k -> "bekleyenOnay".equals(k.getAnahtar())));

        assertEquals(0, kokpit.getNakit().getKasa().compareTo(new BigDecimal("1000")));
        assertEquals(0, kokpit.getNakit().getBanka().compareTo(new BigDecimal("11000")));
        assertEquals(0, kokpit.getNakit().getToplam().compareTo(new BigDecimal("12000")));
        assertNotNull(kokpit.getNakit().getProjeksiyon());
        assertEquals(1, kokpit.getNakit().getProjeksiyon().getGunluk().size());
        assertEquals(1, kokpit.getNakit().getGunlukAkis().size());

        assertEquals(5, kokpit.getAlacakVerecek().getYaslanma().size());
        assertEquals(2, kokpit.getAlacakVerecek().getBorclular().size());
        assertEquals(1, kokpit.getAlacakVerecek().getTedarikciler().size());
        assertEquals(0, kokpit.getAlacakVerecek().getNet().compareTo(new BigDecimal("40000")));

        assertNotNull(kokpit.getKarMarj());
        assertEquals(5, kokpit.getKarMarj().getUrunler().size());
        assertEquals(5, kokpit.getKarMarj().getCariler().size());
        assertEquals(2, kokpit.getKarMarj().getTrend().size());
        assertEquals(2L, kokpit.getKarMarj().getMaliyetsizKalem());

        assertEquals(1, kokpit.getFaturaSiparis().getVadesiGecenFaturalar().size());
        assertEquals(1, kokpit.getFaturaSiparis().getSiparisDurumlari().size());

        verify(dashboardService).karAnaliziGetir(1L, "ADMIN", "BU_AY");
    }

    @Test
    void degisimOncekiSifirsaNullDoner() {
        DashboardOzetDTO ozet = DashboardOzetDTO.builder()
                .kpi(DashboardOzetDTO.KpiDTO.builder()
                        .likidite(DashboardOzetDTO.LikiditeDTO.builder()
                                .deger(BigDecimal.ZERO).kasa(BigDecimal.ZERO).banka(BigDecimal.ZERO)
                                .trendYuzde(BigDecimal.ZERO).sparkline(List.of()).build())
                        .ciro(DashboardOzetDTO.CiroDTO.builder()
                                .deger(new BigDecimal("10000")).onceki(BigDecimal.ZERO)
                                .degisimYuzde(null).build())
                        .kar(DashboardOzetDTO.KarDTO.builder()
                                .deger(new BigDecimal("1000")).onceki(BigDecimal.ZERO)
                                .degisimYuzde(null).marj(new BigDecimal("10")).build())
                        .build())
                .donem(DashboardOzetDTO.DonemDTO.builder().kod("BU_AY").etiket("Bu Ay").build())
                .build();
        when(dashboardService.ozetGetir(any(), any(), any(), any(), any())).thenReturn(ozet);
        when(dashboardService.gorevlerGetir(any(), any())).thenReturn(List.of());

        KokpitDTO kokpit = service.kokpitGetir(1L, "USER", "BU_AY");

        KokpitDTO.KpiDTO ciro = kpi(kokpit, "ciro");
        assertEquals(0, ciro.getOnceki().compareTo(BigDecimal.ZERO));
        assertNull(ciro.getDegisimYuzde());

        KokpitDTO.KpiDTO kar = kpi(kokpit, "brutKar");
        assertNull(kar.getDegisimYuzde());

        KokpitDTO.KpiDTO marj = kpi(kokpit, "marj");
        assertNull(marj.getOnceki());
        assertNull(marj.getDegisimYuzde());
    }

    @Test
    void userRolundeKarMarjNullDoner() {
        stubTemelVeri();

        KokpitDTO user = service.kokpitGetir(1L, "USER", "BU_AY");
        assertNull(user.getKarMarj());
        verify(dashboardService, never()).karAnaliziGetir(any(), any(), any());

        when(dashboardService.karAnaliziGetir(1L, "ADMIN", "BU_AY"))
                .thenReturn(DashboardKarAnaliziDTO.builder()
                        .ozet(DashboardKarAnaliziDTO.OzetDTO.builder()
                                .ciro(new BigDecimal("100")).maliyet(new BigDecimal("60"))
                                .kar(new BigDecimal("40")).marj(new BigDecimal("40"))
                                .maliyetsizKalem(0L).build())
                        .urunler(List.of()).cariler(List.of()).build());
        KokpitDTO admin = service.kokpitGetir(1L, "ADMIN", "BU_AY");
        assertNotNull(admin.getKarMarj());
    }

    @Test
    void hedefRouteVeQueryTasiyor() {
        stubTemelVeri();

        KokpitDTO kokpit = service.kokpitGetir(1L, "USER", "BU_AY");

        KokpitDTO.KpiDTO alacak = kpi(kokpit, "alacak");
        assertEquals("/cari-hesaplar", alacak.getHedef().getRoute());
        assertEquals("alacak", alacak.getHedef().getQuery().get("bakiyeYonu"));

        KokpitDTO.KpiDTO verecek = kpi(kokpit, "verecek");
        assertEquals("borc", verecek.getHedef().getQuery().get("bakiyeYonu"));

        assertEquals("/tahsilat", kpi(kokpit, "vadesiGecen").getHedef().getRoute());

        KokpitDTO.KritikDurumDTO kritikStok = kokpit.getKritikDurum().stream()
                .filter(k -> "kritikStok".equals(k.getAnahtar())).findFirst().orElseThrow();
        assertEquals("/stoklar", kritikStok.getHedef().getRoute());
        assertEquals("1", kritikStok.getHedef().getQuery().get("kritik"));

        assertTrue(kokpit.getOzet().stream().allMatch(k -> k.getHedef() != null));
    }

    @Test
    void bosVerideSifirVeBosListelerDoner() {
        when(dashboardService.gorevlerGetir(any(), any())).thenReturn(List.of());

        KokpitDTO kokpit = service.kokpitGetir(2L, "ADMIN", "GECERSIZ");

        assertNotNull(kokpit);
        assertEquals("BU_AY", kokpit.getDonem().getKod());
        assertNotNull(kokpit.getOzet());
        assertEquals(8, kokpit.getOzet().size());
        assertTrue(kokpit.getOzet().stream()
                .allMatch(k -> k.getDeger() != null && k.getDeger().compareTo(BigDecimal.ZERO) == 0));
        assertTrue(kokpit.getOzet().stream().allMatch(k -> k.getSparkline() != null));

        assertNotNull(kokpit.getNakit());
        assertEquals(0, kokpit.getNakit().getToplam().compareTo(BigDecimal.ZERO));
        assertNotNull(kokpit.getNakit().getProjeksiyon());
        assertTrue(kokpit.getNakit().getProjeksiyon().getGunluk().isEmpty());
        assertTrue(kokpit.getNakit().getGunlukAkis().isEmpty());

        assertNotNull(kokpit.getAlacakVerecek());
        assertEquals(5, kokpit.getAlacakVerecek().getYaslanma().size());
        assertTrue(kokpit.getAlacakVerecek().getBorclular().isEmpty());
        assertTrue(kokpit.getAlacakVerecek().getTedarikciler().isEmpty());

        assertNotNull(kokpit.getKarMarj());
        assertTrue(kokpit.getKarMarj().getUrunler().isEmpty());
        assertTrue(kokpit.getKarMarj().getCariler().isEmpty());

        assertNotNull(kokpit.getFaturaSiparis());
        assertEquals(4, kokpit.getFaturaSiparis().getDurumlar().size());
        assertTrue(kokpit.getFaturaSiparis().getDurumlar().stream()
                .allMatch(k -> k.getAdet() == 0L && k.getTutar().compareTo(BigDecimal.ZERO) == 0));
        assertTrue(kokpit.getFaturaSiparis().getVadesiGecenFaturalar().isEmpty());
    }

    @Test
    void faturaPipelineRepositoryAggregateKullanir() {
        stubTemelVeri();
        when(faturaRepository.kokpitFaturaDurumDagilimi(1L)).thenReturn(List.of(
                Map.of("durum", "KESILDI", "adet", 10L, "tutar", new BigDecimal("10000")),
                Map.of("durum", "TASLAK", "adet", 2L, "tutar", new BigDecimal("2000"))));
        when(faturaRepository.toplamOdenenKesilmisSatis(1L)).thenReturn(new BigDecimal("7000"));
        when(faturaRepository.toplamKalanKesilmisSatis(1L)).thenReturn(new BigDecimal("3000"));
        when(faturaRepository.countOdenenKesilmisSatis(1L)).thenReturn(4L);
        when(faturaRepository.toplamTahsilEdilenKesilmisSatis(1L)).thenReturn(new BigDecimal("5000"));

        KokpitDTO kokpit = service.kokpitGetir(1L, "USER", "BU_AY");

        verify(faturaRepository, times(1)).kokpitFaturaDurumDagilimi(1L);
        verify(faturaRepository, never()).findBySirketIdOrderByTarihDesc(any());
        verify(faturaRepository, never()).basliklariGetir(any());

        List<KokpitDTO.KovaDTO> durumlar = kokpit.getFaturaSiparis().getDurumlar();
        assertEquals(List.of("TASLAK", "TEKLIF", "KESILDI", "IPTAL", "TAHSIL_EDILDI"),
                durumlar.stream().map(KokpitDTO.KovaDTO::getAnahtar).toList());
        assertEquals(10L, durumlar.stream().filter(k -> "KESILDI".equals(k.getAnahtar()))
                .findFirst().orElseThrow().getAdet());
        assertEquals(0, kokpit.getFaturaSiparis().getTahsilatOrani().compareTo(new BigDecimal("70.0")));
    }

    @Test
    void tahsilatOraniSifirCiroIleBolunmez() {
        stubTemelVeri();
        when(faturaRepository.toplamOdenenKesilmisSatis(1L)).thenReturn(BigDecimal.ZERO);
        when(faturaRepository.toplamKalanKesilmisSatis(1L)).thenReturn(BigDecimal.ZERO);

        KokpitDTO kokpit = service.kokpitGetir(1L, "USER", "BU_AY");

        assertEquals(0, kokpit.getFaturaSiparis().getTahsilatOrani().compareTo(BigDecimal.ZERO));
        assertEquals(0, kokpit.getAlacakVerecek().getTahsilatOrani().compareTo(BigDecimal.ZERO));
    }

    // ------------------------------------------------------------------
    // Yardımcılar
    // ------------------------------------------------------------------

    private void stubTemelVeri() {
        when(dashboardService.ozetGetir(any(), any(), any(), any(), any()))
                .thenReturn(DashboardOzetDTO.builder()
                        .kpi(DashboardOzetDTO.KpiDTO.builder()
                                .likidite(DashboardOzetDTO.LikiditeDTO.builder()
                                        .deger(BigDecimal.ZERO).kasa(BigDecimal.ZERO)
                                        .banka(BigDecimal.ZERO).sparkline(List.of()).build())
                                .ciro(DashboardOzetDTO.CiroDTO.builder()
                                        .deger(BigDecimal.ZERO).onceki(BigDecimal.ZERO).build())
                                .kar(DashboardOzetDTO.KarDTO.builder()
                                        .deger(BigDecimal.ZERO).onceki(BigDecimal.ZERO).build())
                                .build())
                        .donem(DashboardOzetDTO.DonemDTO.builder().kod("BU_AY").etiket("Bu Ay").build())
                        .build());
        when(dashboardService.gorevlerGetir(any(), any())).thenReturn(List.of());
    }

    private void stubDoluVeri() {
        when(dashboardService.ozetGetir(1L, null, null, "ADMIN", "BU_AY"))
                .thenReturn(DashboardOzetDTO.builder()
                        .kpi(DashboardOzetDTO.KpiDTO.builder()
                                .likidite(DashboardOzetDTO.LikiditeDTO.builder()
                                        .deger(new BigDecimal("12000"))
                                        .kasa(new BigDecimal("2000"))
                                        .banka(new BigDecimal("10000"))
                                        .trendYuzde(new BigDecimal("5"))
                                        .sparkline(List.of(BigDecimal.ONE, BigDecimal.TWO)).build())
                                .ciro(DashboardOzetDTO.CiroDTO.builder()
                                        .deger(new BigDecimal("100000"))
                                        .onceki(new BigDecimal("80000"))
                                        .degisimYuzde(new BigDecimal("25"))
                                        .hedef(new BigDecimal("200000"))
                                        .yuzde(new BigDecimal("50")).build())
                                .kar(DashboardOzetDTO.KarDTO.builder()
                                        .deger(new BigDecimal("25000"))
                                        .onceki(new BigDecimal("20000"))
                                        .degisimYuzde(new BigDecimal("25"))
                                        .marj(new BigDecimal("25")).build())
                                .build())
                        .gorevSayilari(DashboardOzetDTO.GorevSayilariDTO.builder()
                                .onayBekleyen(3L).vadesiGecen(4L).kritikStok(2L).build())
                        .donem(DashboardOzetDTO.DonemDTO.builder()
                                .kod("BU_AY").etiket("Bu Ay")
                                .baslangic(LocalDate.of(2026, 11, 1))
                                .bitis(LocalDate.of(2026, 11, 30)).build())
                        .build());

        when(dashboardService.grafiklerGetir(1L)).thenReturn(DashboardGrafikDTO.builder()
                .kasaBanka(DashboardGrafikDTO.KasaBankaDTO.builder()
                        .kasa(new BigDecimal("1000")).banka(new BigDecimal("11000")).build())
                .aylikGelirGider(List.of(
                        DashboardDTO.AylikGelirGiderDTO.builder()
                                .ay("2026-10").gelir(new BigDecimal("1000")).gider(new BigDecimal("600")).build(),
                        DashboardDTO.AylikGelirGiderDTO.builder()
                                .ay("2026-11").gelir(new BigDecimal("2000")).gider(new BigDecimal("1500")).build()))
                .gunlukNakitAkisi(List.of(DashboardDTO.GunlukNakitAkisiDTO.builder()
                        .gun("2026-11-05").gelir(new BigDecimal("100")).gider(new BigDecimal("50")).build()))
                .siparisDurumlari(List.of(DashboardGrafikDTO.SiparisDurumDTO.builder()
                        .durum("TEKLIF").adet(3L).build()))
                .build());

        when(dashboardService.gorevlerGetir(1L, "ADMIN")).thenReturn(List.of(
                DashboardGorevDTO.builder().tur("VADESI_GECEN").adet(4L).tutar(new BigDecimal("4000")).build(),
                DashboardGorevDTO.builder().tur("ONAY_IZIN").adet(3L).build(),
                DashboardGorevDTO.builder().tur("KRITIK_STOK").adet(2L).build()));

        when(cariHesapService.toplamNegatifBakiyeGetir(1L)).thenReturn(new BigDecimal("-60000"));
        when(cariHesapService.toplamPozitifBakiyeGetir(1L)).thenReturn(new BigDecimal("20000"));

        when(cariHesapRepository.findTop8BySirketIdAndBakiyeLessThanOrderByBakiyeAsc(eq(1L), any()))
                .thenReturn(List.of(
                        CariHesap.builder().id(10L).ad("Müşteri A").bakiye(new BigDecimal("-40000")).build(),
                        CariHesap.builder().id(11L).ad("Müşteri B").bakiye(new BigDecimal("-20000")).build()));
        when(cariHesapRepository.findTop8BySirketIdAndBakiyeGreaterThanOrderByBakiyeDesc(eq(1L), any()))
                .thenReturn(List.of(CariHesap.builder().id(20L).ad("Tedarikçi A")
                        .bakiye(new BigDecimal("20000")).build()));

        when(raporService.yaslandirmaRaporu(eq(1L), any())).thenReturn(RaporDTO.YaslandirmaRaporDTO.builder()
                .ozet(RaporDTO.YaslandirmaOzetDTO.builder()
                        .kovalar(Map.of(
                                RaporService.KOVA_VADEDI_GELMEMIS, new BigDecimal("10000"),
                                "GUN_0_30", new BigDecimal("5000"),
                                "GUN_31_60", BigDecimal.ZERO,
                                "GUN_61_90", new BigDecimal("1000"),
                                "GUN_90_PLUS", new BigDecimal("500")))
                        .build())
                .build());

        when(raporService.nakitAkisiProjeksiyonu(eq(30), eq(1L)))
                .thenReturn(NakitAkisiProjeksiyonDTO.builder()
                        .baslangicBakiyesi(new BigDecimal("12000"))
                        .toplamBeklenenGiris(new BigDecimal("5000"))
                        .toplamBeklenenCikis(new BigDecimal("3000"))
                        .tahminiBitisBakiyesi(new BigDecimal("14000"))
                        .projeksiyonGunu(30)
                        .gunlukAkis(List.of(NakitAkisiProjeksiyonDTO.GunlukProjeksiyonDTO.builder()
                                .tarih(LocalDate.of(2026, 11, 6))
                                .beklenenGiris(new BigDecimal("5000"))
                                .beklenenCikis(new BigDecimal("3000"))
                                .netAkis(new BigDecimal("2000"))
                                .kumulatifBakiye(new BigDecimal("14000")).build()))
                        .build());

        when(faturaRepository.findVadesiGecen(eq(1L), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(Fatura.builder()
                        .id(100L).faturaNumarasi("FTR-1")
                        .cariHesap(CariHesap.builder().id(10L).ad("Müşteri A").build())
                        .vadeTarihi(LocalDate.now().minusDays(12))
                        .kalanTutar(new BigDecimal("4000")).build()));

        when(dashboardService.karAnaliziGetir(1L, "ADMIN", "BU_AY"))
                .thenReturn(DashboardKarAnaliziDTO.builder()
                        .ozet(DashboardKarAnaliziDTO.OzetDTO.builder()
                                .ciro(new BigDecimal("100000"))
                                .maliyet(new BigDecimal("70000"))
                                .kar(new BigDecimal("30000"))
                                .marj(new BigDecimal("30"))
                                .maliyetsizKalem(2L).build())
                        .urunler(List.of(
                                urun(1L, "Ürün 1", "1000"), urun(2L, "Ürün 2", "900"),
                                urun(3L, "Ürün 3", "800"), urun(4L, "Ürün 4", "700"),
                                urun(5L, "Ürün 5", "600"), urun(6L, "Ürün 6", "500")))
                        .cariler(List.of(
                                cari(1L, "Cari 1", "1000"), cari(2L, "Cari 2", "900"),
                                cari(3L, "Cari 3", "800"), cari(4L, "Cari 4", "700"),
                                cari(5L, "Cari 5", "600"), cari(6L, "Cari 6", "500")))
                        .build());
    }

    private DashboardKarAnaliziDTO.UrunDTO urun(Long id, String ad, String kar) {
        return DashboardKarAnaliziDTO.UrunDTO.builder()
                .stokId(id).ad(ad).ciro(new BigDecimal(kar)).maliyet(BigDecimal.ZERO)
                .kar(new BigDecimal(kar)).marj(new BigDecimal("100")).build();
    }

    private DashboardKarAnaliziDTO.CariDTO cari(Long id, String ad, String kar) {
        return DashboardKarAnaliziDTO.CariDTO.builder()
                .cariId(id).ad(ad).ciro(new BigDecimal(kar)).maliyet(BigDecimal.ZERO)
                .kar(new BigDecimal(kar)).marj(new BigDecimal("100")).build();
    }

    private KokpitDTO.KpiDTO kpi(KokpitDTO kokpit, String anahtar) {
        return kokpit.getOzet().stream()
                .filter(k -> anahtar.equals(k.getAnahtar()))
                .findFirst()
                .orElseThrow();
    }

    private BigDecimal para(KokpitDTO kokpit, String anahtar) {
        return kpi(kokpit, anahtar).getDeger();
    }
}
