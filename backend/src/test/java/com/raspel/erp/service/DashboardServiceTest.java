package com.raspel.erp.service;

import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.dto.sistem.DashboardAkisDTO;
import com.raspel.erp.dto.sistem.DashboardGorevDTO;
import com.raspel.erp.dto.sistem.DashboardIcgoruDTO;
import com.raspel.erp.dto.sistem.DashboardOzetDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.service.sistem.DashboardService;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelMasrafTalepRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.sistem.OnayKararRepository;
import com.raspel.erp.repository.sistem.SirketHedefRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.repository.ticaret.SatinalmaTalepRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.envanter.StokHareket;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.sistem.OnayKarar;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.entity.sistem.SirketHedef;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.entity.ticaret.Siparis;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private CariHesapService cariHesapService;
    @Mock private HareketService hareketService;
    @Mock private HareketRepository hareketRepository;
    @Mock private SiparisRepository siparisRepository;
    @Mock private PersonelRepository personelRepository;
    @Mock private PersonelIzinRepository personelIzinRepository;
    @Mock private PersonelMasrafTalepRepository personelMasrafTalepRepository;
    @Mock private SatinalmaTalepRepository satinalmaTalepRepository;
    @Mock private StokHareketRepository stokHareketRepository;
    @Mock private StokRepository stokRepository;
    @Mock private FaturaRepository faturaRepository;
    @Mock private FaturaKalemRepository faturaKalemRepository;
    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private BankaRepository bankaRepository;
    @Mock private KasaRepository kasaRepository;
    @Mock private MasrafRepository masrafRepository;
    @Mock private IadeRepository iadeRepository;
    @Mock private SirketHedefRepository sirketHedefRepository;
    @Mock private SirketRepository sirketRepository;
    @Mock private TeslimatRepository teslimatRepository;
    @Mock private OnayKararRepository onayKararRepository;
    @InjectMocks private DashboardService dashboardService;

    // ------------------------------------------------------------------
    // Legacy uç (sözleşme korunur)
    // ------------------------------------------------------------------

    @Test
    void dashboardVerileriGetir_returnsData() {
        when(cariHesapService.toplamCariSayisiGetir(1L)).thenReturn(10L);
        when(cariHesapService.toplamBakiyeGetir(1L)).thenReturn(BigDecimal.valueOf(100000));
        when(hareketService.sonHareketleriGetir(5, 1L)).thenReturn(List.of());
        when(faturaRepository.findVadesiGecen(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(faturaRepository.toplamVadesiGecenKalan(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(faturaRepository.toplamKalanVadeAraliginda(any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(faturaRepository.findVadesiYaklasan(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        var result = dashboardService.dashboardVerileriGetir(1L);
        assertEquals(10L, result.getToplamCariSayisi());
        assertEquals(BigDecimal.valueOf(100000), result.getToplamBakiye());
        assertTrue(result.getSonHareketler().isEmpty());
    }

    @Test
    void dashboard_returnsDefaultsOnError() {
        when(cariHesapService.toplamCariSayisiGetir(1L)).thenThrow(new RuntimeException("DB error"));
        var result = dashboardService.dashboardVerileriGetir(1L);
        assertNotNull(result);
        assertEquals(0L, result.getToplamCariSayisi());
    }

    @Test
    void dashboard_alacakYaslandirmayiHesaplar() {
        when(faturaRepository.findVadesiGecen(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(faturaRepository.toplamVadesiGecenKalan(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(faturaRepository.toplamKalanVadeAraliginda(any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(faturaRepository.findVadesiYaklasan(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        var result = dashboardService.dashboardVerileriGetir(1L);
        assertEquals(4, result.getAlacakYaslandirma().size());
        assertEquals("Vadesi Geçti", result.getAlacakYaslandirma().get(0).getAralik());
        assertEquals(0, result.getAlacakYaslandirma().get(0).getTutar().compareTo(BigDecimal.ZERO));
    }

    @Test
    void dashboard_toplamStokDegeriniHesaplar() {
        // Stok degeri artik tek SQL toplaminda hesaplanir (tum stoklar yuklenmez).
        when(stokRepository.toplamStokDegeriBySirketId(1L)).thenReturn(BigDecimal.valueOf(500));
        when(faturaRepository.findVadesiGecen(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(faturaRepository.toplamVadesiGecenKalan(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(faturaRepository.toplamKalanVadeAraliginda(any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(faturaRepository.findVadesiYaklasan(any(), any(), any(), any(), any(), any())).thenReturn(List.of());
        var result = dashboardService.dashboardVerileriGetir(1L);
        assertEquals(0, result.getToplamStokDegeri().compareTo(BigDecimal.valueOf(500)));
    }

    @Test
    void dashboard_iadeOraniniCiroIleHesaplar() {
        Fatura satis = Fatura.builder()
                .tur(Fatura.FaturaTur.SATIS)
                .durum(Fatura.FaturaDurum.KESILDI)
                .genelToplam(BigDecimal.valueOf(10000))
                .build();
        when(faturaRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any())).thenReturn(List.of(satis));

        Iade iade = Iade.builder()
                .tur("SATIS")
                .durum("TAMAMLANDI")
                .tutar(BigDecimal.valueOf(500))
                .build();
        when(iadeRepository.findBySirketIdAndTurAndDurumAndTarihBetween(eq(1L), eq("SATIS"), eq("TAMAMLANDI"), any(), any()))
                .thenReturn(List.of(iade));

        when(faturaRepository.findVadesiGecen(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(faturaRepository.toplamVadesiGecenKalan(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(faturaRepository.toplamKalanVadeAraliginda(any(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(faturaRepository.findVadesiYaklasan(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        var result = dashboardService.dashboardVerileriGetir(1L);
        assertEquals(0, result.getIadeOrani().compareTo(BigDecimal.valueOf(5.0)));
    }

    // ------------------------------------------------------------------
    // /ozet
    // ------------------------------------------------------------------

    @Test
    void ozet_kpiVeGorevSayilariniHesaplar() {
        String bugun = LocalDate.now().toString();
        when(kasaRepository.sumBakiyeBySirketId(1L)).thenReturn(BigDecimal.valueOf(500));
        when(bankaRepository.sumBakiyeBySirketId(1L)).thenReturn(BigDecimal.valueOf(1000));
        when(hareketRepository.gunlukNakitAkisi(any(), eq(1L)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{bugun, BigDecimal.valueOf(100), BigDecimal.valueOf(40)}));
        when(hareketRepository.sumTutarByTurAndHareketTarihi(eq(Hareket.HareketTuru.TAHSILAT), any(), eq(1L)))
                .thenReturn(BigDecimal.valueOf(100));
        when(hareketRepository.sumTutarByTurAndHareketTarihi(eq(Hareket.HareketTuru.ODEME), any(), eq(1L)))
                .thenReturn(BigDecimal.valueOf(50));

        when(cariHesapService.toplamPozitifBakiyeGetir(1L)).thenReturn(BigDecimal.valueOf(4000));
        when(cariHesapService.toplamNegatifBakiyeGetir(1L)).thenReturn(BigDecimal.valueOf(-1000));

        when(sirketHedefRepository.findBySirketIdAndYilAndAy(eq(1L), any(), any()))
                .thenReturn(Optional.of(SirketHedef.builder()
                        .hedefCiro(BigDecimal.valueOf(10000))
                        .hedefKar(BigDecimal.valueOf(2000))
                        .build()));
        Fatura satis = Fatura.builder().tur(Fatura.FaturaTur.SATIS)
                .durum(Fatura.FaturaDurum.KESILDI).genelToplam(BigDecimal.valueOf(5000)).build();
        Fatura alis = Fatura.builder().tur(Fatura.FaturaTur.ALIS)
                .durum(Fatura.FaturaDurum.KESILDI).genelToplam(BigDecimal.valueOf(1000)).build();
        when(faturaRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any())).thenReturn(List.of(satis, alis));
        when(masrafRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any())).thenReturn(List.of());

        when(stokRepository.countKritikStokBySirketId(1L)).thenReturn(3L);
        when(stokRepository.countBySirketId(1L)).thenReturn(40L);
        when(stokRepository.toplamStokDegeriBySirketId(1L)).thenReturn(BigDecimal.valueOf(25000));
        when(siparisRepository.countBySirketIdAndTarih(eq(1L), any())).thenReturn(4L);
        when(siparisRepository.countBySirketIdAndDurumIn(eq(1L), any())).thenReturn(2L);
        when(siparisRepository.countBySirketIdAndDurumNotIn(eq(1L), any())).thenReturn(6L);
        when(personelIzinRepository.countByDurumAndSirketId("BEKLEMEDE", 1L)).thenReturn(1L);
        when(personelMasrafTalepRepository.countBySirketIdAndDurum(1L, "BEKLEMEDE")).thenReturn(1L);
        when(satinalmaTalepRepository.countBySirketIdAndDurum(1L, "TASLAK")).thenReturn(1L);
        when(teslimatRepository.countBySirketIdAndBeklenenTeslimTarihiAndDurumNotIn(eq(1L), any(), any())).thenReturn(5L);
        when(faturaRepository.countVadesiGecen(eq(1L), any(), any(), any())).thenReturn(2L);
        when(faturaRepository.countVadesiYaklasan(eq(1L), any(), any(), any(), any())).thenReturn(3L);
        when(sirketRepository.findById(1L)).thenReturn(Optional.of(Sirket.builder().ad("Test AŞ").build()));

        DashboardOzetDTO ozet = dashboardService.ozetGetir(1L, 9L, "Ali Veli", "ADMIN");

        assertEquals("Ali Veli", ozet.getHero().getKullaniciAd());
        assertEquals("Test AŞ", ozet.getHero().getSirketAd());
        assertTrue(ozet.getHero().getCanli());

        assertEquals(0, ozet.getKpi().getLikidite().getDeger().compareTo(BigDecimal.valueOf(1500)));
        assertEquals(0, ozet.getKpi().getLikidite().getKasa().compareTo(BigDecimal.valueOf(500)));
        assertEquals(0, ozet.getKpi().getLikidite().getBanka().compareTo(BigDecimal.valueOf(1000)));
        // Net 60 / likidite 1500 = %4
        assertEquals(0, ozet.getKpi().getLikidite().getTrendYuzde().compareTo(BigDecimal.valueOf(4.0)));
        assertEquals(7, ozet.getKpi().getLikidite().getSparkline().size());
        assertEquals(0, ozet.getKpi().getLikidite().getSparkline().get(6).compareTo(BigDecimal.valueOf(60)));

        assertEquals(0, ozet.getKpi().getNetCari().getDeger().compareTo(BigDecimal.valueOf(3000)));
        assertEquals(0, ozet.getKpi().getNetCari().getBorc().compareTo(BigDecimal.valueOf(1000)));

        assertEquals(0, ozet.getKpi().getCiro().getYuzde().compareTo(BigDecimal.valueOf(50.0)));
        assertEquals(0, ozet.getKpi().getKar().getDeger().compareTo(BigDecimal.valueOf(4000)));
        assertEquals(0, ozet.getKpi().getKar().getMarj().compareTo(BigDecimal.valueOf(80.0)));

        assertEquals(40L, ozet.getStok().getCesit());
        assertEquals(3L, ozet.getStok().getKritikAdet());

        assertEquals(5L, ozet.getGorevSayilari().getOnayBekleyen());
        assertEquals(2L, ozet.getGorevSayilari().getVadesiGecen());
        assertEquals(3L, ozet.getGorevSayilari().getKritikStok());
        assertEquals(6L, ozet.getGorevSayilari().getBekleyenTeslimat());
        assertEquals(3L, ozet.getGorevSayilari().getVadesiYaklasan());

        assertEquals(4L, ozet.getBugun().getSiparis());
        assertEquals(5L, ozet.getBugun().getTeslimat());
        assertEquals(0, ozet.getBugun().getTahsilat().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, ozet.getBugun().getOdeme().compareTo(BigDecimal.valueOf(50)));
    }

    @Test
    void ozet_bosVerideSifirDoner() {
        DashboardOzetDTO ozet = dashboardService.ozetGetir(1L, 9L, "Ali", "USER");

        assertEquals(0, ozet.getKpi().getLikidite().getDeger().compareTo(BigDecimal.ZERO));
        assertEquals(7, ozet.getKpi().getLikidite().getSparkline().size());
        assertEquals(0, ozet.getKpi().getCiro().getYuzde().compareTo(BigDecimal.ZERO));
        assertEquals(0L, ozet.getGorevSayilari().getOnayBekleyen());
        assertNull(ozet.getHero().getSirketAd());
    }

    // ------------------------------------------------------------------
    // /gorevler
    // ------------------------------------------------------------------

    private void gorevSayaclariniDoldur() {
        when(personelIzinRepository.countByDurumAndSirketId("BEKLEMEDE", 1L)).thenReturn(2L);
        when(personelMasrafTalepRepository.countBySirketIdAndDurum(1L, "BEKLEMEDE")).thenReturn(1L);
        when(personelMasrafTalepRepository.sumTutarBySirketIdAndDurum(1L, "BEKLEMEDE")).thenReturn(BigDecimal.valueOf(750));
        when(satinalmaTalepRepository.countBySirketIdAndDurum(1L, "TASLAK")).thenReturn(3L);
        when(siparisRepository.countBySirketIdAndDurumIn(eq(1L), any())).thenReturn(2L);
        when(siparisRepository.sumTutarBySirketIdAndDurumIn(eq(1L), any())).thenReturn(BigDecimal.valueOf(4200));
        when(faturaRepository.countVadesiGecen(eq(1L), any(), any(), any())).thenReturn(4L);
        when(faturaRepository.toplamVadesiGecenKalan(eq(1L), any(), any(), any())).thenReturn(BigDecimal.valueOf(900));
        when(faturaRepository.countVadesiYaklasan(eq(1L), any(), any(), any(), any())).thenReturn(5L);
        when(faturaRepository.toplamKalanVadeAraliginda(eq(1L), any(), any(), any(), any())).thenReturn(BigDecimal.valueOf(1100));
        when(stokRepository.countKritikStokBySirketId(1L)).thenReturn(6L);
        when(siparisRepository.countBySirketIdAndDurumNotIn(eq(1L), any())).thenReturn(7L);
    }

    @Test
    void gorevler_adminTumGorevleriOncelikSiraliDoner() {
        gorevSayaclariniDoldur();

        List<DashboardGorevDTO> gorevler = dashboardService.gorevlerGetir(1L, "ADMIN");

        assertEquals(8, gorevler.size());
        assertEquals(List.of("VADESI_GECEN", "ONAY_IZIN", "ONAY_MASRAF", "ONAY_SATINALMA",
                        "ONAY_SIPARIS", "KRITIK_STOK", "BEKLEYEN_TESLIMAT", "VADESI_YAKLASAN"),
                gorevler.stream().map(DashboardGorevDTO::getTur).toList());
        assertEquals(95, gorevler.get(0).getOncelik());
        for (int i = 1; i < gorevler.size(); i++) {
            assertTrue(gorevler.get(i - 1).getOncelik() >= gorevler.get(i).getOncelik());
        }
        assertEquals("/tahsilat", gorevler.get(0).getRoute());
        assertEquals("/onaylar?tur=IZIN", gorevler.get(1).getRoute());
        assertEquals("/stoklar?kritik=1", gorevler.get(5).getRoute());
        assertEquals("/teslimatlar", gorevler.get(6).getRoute());
    }

    @Test
    void gorevler_userYalnizVadeStokTeslimatGorur() {
        when(faturaRepository.countVadesiGecen(eq(1L), any(), any(), any())).thenReturn(4L);
        when(faturaRepository.toplamVadesiGecenKalan(eq(1L), any(), any(), any())).thenReturn(BigDecimal.valueOf(900));
        when(faturaRepository.countVadesiYaklasan(eq(1L), any(), any(), any(), any())).thenReturn(5L);
        when(faturaRepository.toplamKalanVadeAraliginda(eq(1L), any(), any(), any(), any())).thenReturn(BigDecimal.valueOf(1100));
        when(stokRepository.countKritikStokBySirketId(1L)).thenReturn(6L);
        when(siparisRepository.countBySirketIdAndDurumNotIn(eq(1L), any())).thenReturn(7L);

        List<DashboardGorevDTO> gorevler = dashboardService.gorevlerGetir(1L, "USER");

        assertEquals(4, gorevler.size());
        assertTrue(gorevler.stream().noneMatch(g -> g.getTur().startsWith("ONAY_")));
        assertEquals(List.of("VADESI_GECEN", "KRITIK_STOK", "BEKLEYEN_TESLIMAT", "VADESI_YAKLASAN"),
                gorevler.stream().map(DashboardGorevDTO::getTur).toList());
        verifyNoInteractions(personelIzinRepository, personelMasrafTalepRepository, satinalmaTalepRepository);
    }

    @Test
    void gorevler_sifirAdetliGorevDonmez() {
        List<DashboardGorevDTO> gorevler = dashboardService.gorevlerGetir(1L, "ADMIN");

        assertTrue(gorevler.isEmpty());
    }

    // ------------------------------------------------------------------
    // /grafikler
    // ------------------------------------------------------------------

    @Test
    void grafikler_bosVerideBosListelerDoner() {
        var grafik = dashboardService.grafiklerGetir(1L);

        assertTrue(grafik.getAylikGelirGider().isEmpty());
        assertTrue(grafik.getGunlukNakitAkisi().isEmpty());
        assertTrue(grafik.getEnCokSatanlar().isEmpty());
        assertTrue(grafik.getKategoriSatislari().isEmpty());
        assertEquals(4, grafik.getAlacakYaslandirma().size());
        assertEquals(0, grafik.getKasaBanka().getKasa().compareTo(BigDecimal.ZERO));
        assertEquals(0, grafik.getHedef().getCiro().compareTo(BigDecimal.ZERO));
    }

    @Test
    void grafikler_doluVerideMevcutHesaplariKullanir() {
        when(hareketRepository.aylikGelirGider(any(), eq(1L)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{"2026-10", BigDecimal.valueOf(1000), BigDecimal.valueOf(400)}));
        when(hareketRepository.gunlukNakitAkisi(any(), eq(1L)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{"2026-10-28", BigDecimal.valueOf(200), BigDecimal.valueOf(50)}));
        when(stokHareketRepository.enCokSatanlarBySirket(1L)).thenReturn(List.of(
                new java.util.HashMap<>(Map.of("stokAd", "Ürün A", "stokKodu", "A1",
                        "satisMiktari", BigDecimal.valueOf(12)))));
        when(faturaKalemRepository.kategoriSatislari(1L, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI))
                .thenReturn(java.util.Collections.singletonList(new Object[]{"Gıda", BigDecimal.valueOf(3000)}));
        when(faturaRepository.toplamVadesiGecenKalan(eq(1L), any(), any(), any())).thenReturn(BigDecimal.valueOf(400));
        when(faturaRepository.toplamKalanVadeAraliginda(eq(1L), any(), any(), any(), any())).thenReturn(BigDecimal.valueOf(100));
        when(bankaRepository.sumBakiyeBySirketId(1L)).thenReturn(BigDecimal.valueOf(800));
        when(kasaRepository.sumBakiyeBySirketId(1L)).thenReturn(BigDecimal.valueOf(200));
        when(sirketHedefRepository.findBySirketIdAndYilAndAy(eq(1L), any(), any()))
                .thenReturn(Optional.of(SirketHedef.builder()
                        .hedefCiro(BigDecimal.valueOf(20000))
                        .hedefKar(BigDecimal.valueOf(5000))
                        .build()));
        Fatura satis = Fatura.builder().tur(Fatura.FaturaTur.SATIS)
                .durum(Fatura.FaturaDurum.KESILDI).genelToplam(BigDecimal.valueOf(12000)).build();
        when(faturaRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any())).thenReturn(List.of(satis));

        var grafik = dashboardService.grafiklerGetir(1L);

        assertEquals(1, grafik.getAylikGelirGider().size());
        assertEquals(1, grafik.getGunlukNakitAkisi().size());
        assertEquals("Ürün A", grafik.getEnCokSatanlar().get(0).getStokAd());
        assertEquals("Gıda", grafik.getKategoriSatislari().get(0).getKategori());
        assertEquals(0, grafik.getAlacakYaslandirma().get(0).getTutar().compareTo(BigDecimal.valueOf(400)));
        assertEquals(0, grafik.getKasaBanka().getBanka().compareTo(BigDecimal.valueOf(800)));
        assertEquals(0, grafik.getKasaBanka().getKasa().compareTo(BigDecimal.valueOf(200)));
        assertEquals(0, grafik.getHedef().getGerceklesen().compareTo(BigDecimal.valueOf(12000)));
        assertEquals(0, grafik.getHedef().getCiro().compareTo(BigDecimal.valueOf(20000)));
        assertEquals(0, grafik.getHedef().getKar().compareTo(BigDecimal.valueOf(12000)));
        assertEquals(0, grafik.getHedef().getHedefKar().compareTo(BigDecimal.valueOf(5000)));
    }

    // ------------------------------------------------------------------
    // /akis
    // ------------------------------------------------------------------

    @Test
    void akis_kaynaklariBirlestirirTariheGoreSiralar() {
        LocalDateTime simdi = LocalDateTime.now();
        when(onayKararRepository.findBySirketIdOrderByOlusturmaTarihiDesc(eq(1L), any()))
                .thenReturn(List.of(OnayKarar.builder().tur("IZIN").karar("ONAY")
                        .kararVerenAd("Yönetici").olusturmaTarihi(simdi.minusHours(1)).build()));
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDescOlusturmaTarihiDesc(eq(1L), any()))
                .thenReturn(List.of(Hareket.builder().tur(Hareket.HareketTuru.TAHSILAT)
                        .tutar(BigDecimal.valueOf(250)).hareketTarihi(LocalDate.now())
                        .olusturmaTarihi(simdi).build()));
        when(faturaRepository.findBySirketIdOrderByTarihDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(Fatura.builder().faturaNumarasi("F-1")
                        .tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                        .genelToplam(BigDecimal.valueOf(500))
                        .tarih(LocalDate.now()).olusturmaTarihi(simdi.minusHours(2)).build())));
        when(siparisRepository.findBySirketIdOrderByTarihDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(Siparis.builder().siparisNo("S-1")
                        .durum("TEKLIF").genelToplam(BigDecimal.valueOf(700))
                        .tarih(LocalDate.now()).olusturmaTarihi(simdi.minusHours(3)).build())));
        Stok stok = Stok.builder().ad("Ürün A").stokKodu("A1").build();
        when(stokHareketRepository.findByStokSirketId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(StokHareket.builder().stok(stok).tur("CIKIS")
                        .miktar(BigDecimal.ONE).hareketTarihi(LocalDate.now())
                        .olusturmaTarihi(simdi.minusHours(4)).build())));

        List<DashboardAkisDTO> akis = dashboardService.akisGetir(1L, 10);

        assertEquals(5, akis.size());
        assertEquals(List.of("TAHSILAT", "ONAY", "FATURA", "SIPARIS", "STOK"),
                akis.stream().map(DashboardAkisDTO::getTur).toList());
        for (int i = 1; i < akis.size(); i++) {
            assertFalse(akis.get(i).getTarih().isAfter(akis.get(i - 1).getTarih()));
        }
    }

    @Test
    void akis_limitUygularVeEnFazla50Okur() {
        LocalDateTime simdi = LocalDateTime.now();
        when(onayKararRepository.findBySirketIdOrderByOlusturmaTarihiDesc(eq(1L), any()))
                .thenReturn(List.of());
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDescOlusturmaTarihiDesc(eq(1L), any()))
                .thenReturn(List.of(
                        Hareket.builder().tur(Hareket.HareketTuru.TAHSILAT).tutar(BigDecimal.ONE)
                                .hareketTarihi(LocalDate.now()).olusturmaTarihi(simdi).build(),
                        Hareket.builder().tur(Hareket.HareketTuru.ODEME).tutar(BigDecimal.ONE)
                                .hareketTarihi(LocalDate.now()).olusturmaTarihi(simdi.minusHours(1)).build()));
        when(faturaRepository.findBySirketIdOrderByTarihDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(siparisRepository.findBySirketIdOrderByTarihDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(stokHareketRepository.findByStokSirketId(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of()));

        List<DashboardAkisDTO> akis = dashboardService.akisGetir(1L, 1);

        assertEquals(1, akis.size());
        assertEquals("TAHSILAT", akis.get(0).getTur());
        verify(hareketRepository).findBySirketIdOrderByHareketTarihiDescOlusturmaTarihiDesc(eq(1L), eq(PageRequest.of(0, 1)));
    }

    @Test
    void akis_limitUstSiniriAsmaz() {
        when(onayKararRepository.findBySirketIdOrderByOlusturmaTarihiDesc(eq(1L), any())).thenReturn(List.of());
        when(hareketRepository.findBySirketIdOrderByHareketTarihiDescOlusturmaTarihiDesc(eq(1L), any())).thenReturn(List.of());
        when(faturaRepository.findBySirketIdOrderByTarihDesc(eq(1L), any())).thenReturn(new PageImpl<>(List.of()));
        when(siparisRepository.findBySirketIdOrderByTarihDesc(eq(1L), any())).thenReturn(new PageImpl<>(List.of()));
        when(stokHareketRepository.findByStokSirketId(eq(1L), any())).thenReturn(new PageImpl<>(List.of()));

        dashboardService.akisGetir(1L, 500);

        verify(onayKararRepository).findBySirketIdOrderByOlusturmaTarihiDesc(eq(1L), eq(PageRequest.of(0, 50)));
    }

    @Test
    void akis_tenantYoksaBosDoner() {
        assertTrue(dashboardService.akisGetir(null, 20).isEmpty());
        verifyNoInteractions(onayKararRepository, hareketRepository, faturaRepository, siparisRepository, stokHareketRepository);
    }

    // ------------------------------------------------------------------
    // /icgoruler
    // ------------------------------------------------------------------

    @Test
    void icgoru_esikleriSeviyeyeCevirirVeEnFazlaUcDoner() {
        when(sirketHedefRepository.findBySirketIdAndYilAndAy(eq(1L), any(), any()))
                .thenReturn(Optional.of(SirketHedef.builder()
                        .hedefCiro(BigDecimal.valueOf(10000))
                        .hedefKar(BigDecimal.valueOf(2000))
                        .build()));
        Fatura satis = Fatura.builder().tur(Fatura.FaturaTur.SATIS)
                .durum(Fatura.FaturaDurum.KESILDI).genelToplam(BigDecimal.valueOf(2000)).build();
        when(faturaRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any())).thenReturn(List.of(satis));
        when(faturaRepository.countVadesiGecen(eq(1L), any(), any(), any())).thenReturn(6L);
        when(faturaRepository.toplamVadesiGecenKalan(eq(1L), any(), any(), any())).thenReturn(BigDecimal.valueOf(900));
        when(stokRepository.countKritikStokBySirketId(1L)).thenReturn(9L);
        String bugun = LocalDate.now().toString();
        when(hareketRepository.gunlukNakitAkisi(any(), eq(1L)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{bugun, BigDecimal.valueOf(10), BigDecimal.valueOf(60)}));

        List<DashboardIcgoruDTO> icgoruler = dashboardService.icgorulerGetir(1L);

        assertEquals(3, icgoruler.size());
        assertEquals("KRITIK", icgoruler.get(0).getSeviye());
        assertEquals("KRITIK", icgoruler.get(1).getSeviye());
        assertEquals("WARN", icgoruler.get(2).getSeviye());
        assertTrue(icgoruler.get(0).getMesaj().contains("20"));
        assertTrue(icgoruler.get(1).getMesaj().contains("6"));
        assertEquals("/tahsilat", icgoruler.get(1).getRoute());
    }

    @Test
    void icgoru_hedefYoksaCiroKuraliAtlanir() {
        List<DashboardIcgoruDTO> icgoruler = dashboardService.icgorulerGetir(1L);

        assertTrue(icgoruler.isEmpty());
    }

    @Test
    void icgoru_gecenFaturaTekVeKritikStokDokuzWarnDoner() {
        when(faturaRepository.countVadesiGecen(eq(1L), any(), any(), any())).thenReturn(1L);
        when(faturaRepository.toplamVadesiGecenKalan(eq(1L), any(), any(), any())).thenReturn(BigDecimal.valueOf(100));
        when(stokRepository.countKritikStokBySirketId(1L)).thenReturn(9L);
        when(hareketRepository.gunlukNakitAkisi(any(), eq(1L)))
                .thenReturn(java.util.Collections.singletonList(new Object[]{LocalDate.now().toString(),
                        BigDecimal.valueOf(10), BigDecimal.valueOf(60)}));

        List<DashboardIcgoruDTO> icgoruler = dashboardService.icgorulerGetir(1L);

        assertEquals(3, icgoruler.size());
        assertTrue(icgoruler.stream().allMatch(i -> "WARN".equals(i.getSeviye())));
        assertTrue(icgoruler.stream().anyMatch(i -> i.getRoute().equals("/stoklar?kritik=1")));
        assertTrue(icgoruler.stream().anyMatch(i -> i.getRoute().equals("/kasa")));
    }
}
