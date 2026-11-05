package com.raspel.erp.service;

import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.dto.sistem.DashboardAkisDTO;
import com.raspel.erp.dto.sistem.DashboardGorevDTO;
import com.raspel.erp.dto.sistem.DashboardIcgoruDTO;
import com.raspel.erp.dto.sistem.DashboardKarAnaliziDTO;
import com.raspel.erp.dto.sistem.DashboardOzetDTO;
import com.raspel.erp.dto.sistem.DashboardVadeDTO;
import com.raspel.erp.repository.ticaret.KarAnalizOzetProjeksiyon;
import com.raspel.erp.repository.ticaret.KarAnalizSatirProjeksiyon;
import com.raspel.erp.repository.ticaret.VadeTakvimProjeksiyon;
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
import com.raspel.erp.repository.finans.ButceRepository;
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
import com.raspel.erp.entity.finans.Butce;
import com.raspel.erp.entity.finans.Masraf;
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
    @Mock private ButceRepository butceRepository;
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
        assertTrue(grafik.getSiparisDurumlari().isEmpty());
        assertTrue(grafik.getButceGerceklesen().isEmpty());
        assertNotNull(grafik.getStokDegeri());
        assertEquals(0, grafik.getStokDegeri().getToplamDeger().compareTo(BigDecimal.ZERO));
        assertEquals(0L, grafik.getStokDegeri().getKritikAdet());
        assertTrue(grafik.getStokDegeri().getKategoriler().isEmpty());
    }

    @Test
    void grafikler_siparisDurumDagiliminiAdetAzalanDoner() {
        when(siparisRepository.durumDagilimi(1L)).thenReturn(List.of(
                new Object[]{"TESLIM_EDILDI", 5L},
                new Object[]{"TEKLIF", 3L},
                new Object[]{null, 1L}));

        var grafik = dashboardService.grafiklerGetir(1L);

        assertEquals(3, grafik.getSiparisDurumlari().size());
        assertEquals("TESLIM_EDILDI", grafik.getSiparisDurumlari().get(0).getDurum());
        assertEquals(5L, grafik.getSiparisDurumlari().get(0).getAdet());
        assertEquals("TEKLIF", grafik.getSiparisDurumlari().get(1).getDurum());
        // Durumu boş kayıt uydurma bir durum adı değil, tek "DIGER" dilimi olur.
        assertEquals("DIGER", grafik.getSiparisDurumlari().get(2).getDurum());
        assertEquals(1L, grafik.getSiparisDurumlari().get(2).getAdet());
    }

    @Test
    void grafikler_butceGercekleseniBirlestirirVeAltiIleSinirlar() {
        int yil = LocalDate.now().getYear();
        int ay = LocalDate.now().getMonthValue();
        int oncekiAy = ay == 1 ? 12 : ay - 1;

        when(butceRepository.findBySirketIdAndYilAndTurOrderByKategoriAscAyAsc(1L, yil, "GIDER"))
                .thenReturn(List.of(
                        butce(yil, ay, "Kira", 1000),
                        butce(yil, ay, " kira ", 500),
                        butce(yil, ay, "Personel", 900),
                        butce(yil, ay, "Yazılım", 800),
                        butce(yil, ay, "Enerji", 700),
                        butce(yil, ay, "Kargo", 600),
                        butce(yil, ay, "Diğer", 500),
                        butce(yil, ay, "Pazarlama", 400),
                        // Önceki ayın bütçesi grafiğe girmez.
                        butce(yil, oncekiAy, "Eski Ay", 9999)));

        when(masrafRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any()))
                .thenReturn(List.of(
                        masraf(" KIRA ", 200),
                        masraf("Reklam", 2000)));

        var grafik = dashboardService.grafiklerGetir(1L);
        var satirlar = grafik.getButceGerceklesen();

        // 9 bütçe + 1 masraf kategorisi -> en çok 6 dilim.
        assertEquals(6, satirlar.size());
        // Sıralama max(bütçe, gerçekleşen) azalan.
        assertEquals("Reklam", satirlar.get(0).getKategori());
        assertEquals(0, satirlar.get(0).getButce().compareTo(BigDecimal.ZERO));
        assertEquals(0, satirlar.get(0).getGerceklesen().compareTo(BigDecimal.valueOf(2000)));
        // "Kira" + " kira " büyük/küçük harf ve boşluk duyarsız birleşir.
        assertEquals("Kira", satirlar.get(1).getKategori());
        assertEquals(0, satirlar.get(1).getButce().compareTo(BigDecimal.valueOf(1500)));
        assertEquals(0, satirlar.get(1).getGerceklesen().compareTo(BigDecimal.valueOf(200)));
        assertEquals(List.of("Reklam", "Kira", "Personel", "Yazılım", "Enerji", "Kargo"),
                satirlar.stream().map(g -> g.getKategori()).toList());
        assertTrue(satirlar.stream().noneMatch(g -> "Eski Ay".equals(g.getKategori())));
    }

    @Test
    void grafikler_stokDegeriVeKategoriDagiliminiDoner() {
        when(stokRepository.toplamStokDegeriBySirketId(1L)).thenReturn(BigDecimal.valueOf(50000));
        when(stokRepository.countKritikStokBySirketId(1L)).thenReturn(7L);
        when(stokRepository.kategoriDegerDagilimi(eq(1L), any())).thenReturn(List.of(
                new Object[]{"Gıda", BigDecimal.valueOf(30000), 12L},
                new Object[]{"Temizlik", BigDecimal.valueOf(20000), 5L}));

        var stok = dashboardService.grafiklerGetir(1L).getStokDegeri();

        assertEquals(0, stok.getToplamDeger().compareTo(BigDecimal.valueOf(50000)));
        assertEquals(7L, stok.getKritikAdet());
        assertEquals(2, stok.getKategoriler().size());
        assertEquals("Gıda", stok.getKategoriler().get(0).getKategori());
        assertEquals(0, stok.getKategoriler().get(0).getDeger().compareTo(BigDecimal.valueOf(30000)));
        assertEquals(12L, stok.getKategoriler().get(0).getAdet());
        // Limit DB'de uygulanır: en değerli 6 kategori istenir.
        verify(stokRepository).kategoriDegerDagilimi(1L, PageRequest.of(0, 6));
    }

    private Butce butce(int yil, int ay, String kategori, long tutar) {
        return Butce.builder()
                .yil(yil).ay(ay).tur("GIDER").kategori(kategori)
                .tutar(BigDecimal.valueOf(tutar))
                .build();
    }

    private Masraf masraf(String kategori, long tutar) {
        return Masraf.builder()
                .tarih(LocalDate.now().withDayOfMonth(1))
                .kategori(kategori)
                .tutar(BigDecimal.valueOf(tutar))
                .build();
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

    // ------------------------------------------------------------------
    // /ozet?donem=
    // ------------------------------------------------------------------

    private Fatura satisFaturasi(long tutar) {
        return Fatura.builder().tur(Fatura.FaturaTur.SATIS)
                .durum(Fatura.FaturaDurum.KESILDI).genelToplam(BigDecimal.valueOf(tutar)).build();
    }

    private Fatura alisFaturasi(long tutar) {
        return Fatura.builder().tur(Fatura.FaturaTur.ALIS)
                .durum(Fatura.FaturaDurum.KESILDI).genelToplam(BigDecimal.valueOf(tutar)).build();
    }

    /** Özet için kullanılan tarih aralıklarını yakalar (ilk çağrı dönem, ikinci çağrı önceki dönem). */
    private void araliklariDogrula(LocalDate beklenenBas, LocalDate beklenenBit,
                                   LocalDate beklenenOncekiBas, LocalDate beklenenOncekiBit) {
        var basCap = org.mockito.ArgumentCaptor.forClass(LocalDate.class);
        var bitCap = org.mockito.ArgumentCaptor.forClass(LocalDate.class);
        verify(faturaRepository, times(2)).findBySirketIdAndTarihBetween(eq(1L), basCap.capture(), bitCap.capture());
        assertEquals(List.of(beklenenBas, beklenenOncekiBas), basCap.getAllValues());
        assertEquals(List.of(beklenenBit, beklenenOncekiBit), bitCap.getAllValues());
    }

    @Test
    void ozetDonem_buAyAraliginiVeOncekiAyiKullanir() {
        LocalDate bugun = LocalDate.now();
        LocalDate ayBas = bugun.withDayOfMonth(1);
        LocalDate aySon = bugun.withDayOfMonth(bugun.lengthOfMonth());
        LocalDate gecenAyBas = ayBas.minusMonths(1);
        LocalDate gecenAySon = ayBas.minusDays(1);

        when(faturaRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any()))
                .thenReturn(List.of(satisFaturasi(5000), alisFaturasi(1000)))
                .thenReturn(List.of(satisFaturasi(4000)));
        when(masrafRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any())).thenReturn(List.of());

        DashboardOzetDTO ozet = dashboardService.ozetGetir(1L, 9L, "Ali", "ADMIN", "BU_AY");

        assertEquals("BU_AY", ozet.getDonem().getKod());
        assertEquals("Bu Ay", ozet.getDonem().getEtiket());
        assertEquals(ayBas, ozet.getDonem().getBaslangic());
        assertEquals(aySon, ozet.getDonem().getBitis());

        assertEquals(0, ozet.getKpi().getCiro().getDeger().compareTo(BigDecimal.valueOf(5000)));
        assertEquals(0, ozet.getKpi().getCiro().getOnceki().compareTo(BigDecimal.valueOf(4000)));
        // (5000 - 4000) / 4000 * 100 = 25
        assertEquals(0, ozet.getKpi().getCiro().getDegisimYuzde().compareTo(BigDecimal.valueOf(25.0)));

        assertEquals(0, ozet.getKpi().getKar().getDeger().compareTo(BigDecimal.valueOf(4000)));
        assertEquals(0, ozet.getKpi().getKar().getOnceki().compareTo(BigDecimal.valueOf(4000)));
        assertEquals(0, ozet.getKpi().getKar().getDegisimYuzde().compareTo(BigDecimal.ZERO));

        araliklariDogrula(ayBas, aySon, gecenAyBas, gecenAySon);
    }

    @Test
    void ozetDonem_gecenAyiVeIkiAyOncekiAraligiKullanir() {
        LocalDate bugun = LocalDate.now();
        LocalDate bas = bugun.minusMonths(1).withDayOfMonth(1);
        LocalDate bit = bas.withDayOfMonth(bas.lengthOfMonth());

        DashboardOzetDTO ozet = dashboardService.ozetGetir(1L, 9L, "Ali", "ADMIN", "gecen_ay");

        // Kod büyük harfe normalize edilir.
        assertEquals("GECEN_AY", ozet.getDonem().getKod());
        assertEquals("Geçen Ay", ozet.getDonem().getEtiket());
        assertEquals(bas, ozet.getDonem().getBaslangic());
        assertEquals(bit, ozet.getDonem().getBitis());
        // Diğer dönemlerde aylık hedef yoktur.
        assertNull(ozet.getKpi().getCiro().getHedef());
        assertNull(ozet.getKpi().getCiro().getYuzde());
        assertNull(ozet.getKpi().getKar().getHedef());
        verifyNoInteractions(sirketHedefRepository);

        araliklariDogrula(bas, bit, bas.minusMonths(1), bas.minusDays(1));
    }

    @Test
    void ozetDonem_buCeyrekAraliginiVeOncekiCeyregiKullanir() {
        LocalDate bugun = LocalDate.now();
        int ilkAy = ((bugun.getMonthValue() - 1) / 3) * 3 + 1;
        LocalDate bas = LocalDate.of(bugun.getYear(), ilkAy, 1);
        LocalDate bit = bas.plusMonths(3).minusDays(1);

        DashboardOzetDTO ozet = dashboardService.ozetGetir(1L, 9L, "Ali", "ADMIN", "BU_CEYREK");

        assertEquals("BU_CEYREK", ozet.getDonem().getKod());
        assertEquals("Bu Çeyrek", ozet.getDonem().getEtiket());
        assertEquals(bas, ozet.getDonem().getBaslangic());
        assertEquals(bit, ozet.getDonem().getBitis());
        assertNull(ozet.getKpi().getCiro().getHedef());

        araliklariDogrula(bas, bit, bas.minusMonths(3), bas.minusDays(1));
    }

    @Test
    void ozetDonem_buYilAraliginiVeGecenYiliKullanir() {
        int yil = LocalDate.now().getYear();
        LocalDate bas = LocalDate.of(yil, 1, 1);
        LocalDate bit = LocalDate.of(yil, 12, 31);

        DashboardOzetDTO ozet = dashboardService.ozetGetir(1L, 9L, "Ali", "ADMIN", "BU_YIL");

        assertEquals("BU_YIL", ozet.getDonem().getKod());
        assertEquals("Bu Yıl", ozet.getDonem().getEtiket());
        assertEquals(bas, ozet.getDonem().getBaslangic());
        assertEquals(bit, ozet.getDonem().getBitis());
        assertNull(ozet.getKpi().getKar().getHedef());

        araliklariDogrula(bas, bit, bas.minusYears(1), bit.minusYears(1));
    }

    @Test
    void ozetDonem_gecersizDonemBusinessExceptionFirlatir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> dashboardService.ozetGetir(1L, 9L, "Ali", "ADMIN", "DUN"));
        verifyNoInteractions(faturaRepository, masrafRepository, sirketHedefRepository);
    }

    @Test
    void ozetDonem_bosDonemBuAyaDuser() {
        DashboardOzetDTO ozet = dashboardService.ozetGetir(1L, 9L, "Ali", "ADMIN", " ");

        assertEquals("BU_AY", ozet.getDonem().getKod());
    }

    @Test
    void ozetDonem_oncekiSifirsaDegisimYuzdeNullDoner() {
        when(faturaRepository.findBySirketIdAndTarihBetween(eq(1L), any(), any()))
                .thenReturn(List.of(satisFaturasi(5000)))
                .thenReturn(List.of());

        DashboardOzetDTO ozet = dashboardService.ozetGetir(1L, 9L, "Ali", "ADMIN", "BU_AY");

        assertEquals(0, ozet.getKpi().getCiro().getOnceki().compareTo(BigDecimal.ZERO));
        assertNull(ozet.getKpi().getCiro().getDegisimYuzde());
        assertNull(ozet.getKpi().getKar().getDegisimYuzde());
    }

    @Test
    void ozetDonem_hedefYalnizcaBuAydaDolu() {
        when(sirketHedefRepository.findBySirketIdAndYilAndAy(eq(1L), any(), any()))
                .thenReturn(Optional.of(SirketHedef.builder()
                        .hedefCiro(BigDecimal.valueOf(10000))
                        .hedefKar(BigDecimal.valueOf(2000))
                        .build()));

        DashboardOzetDTO buAy = dashboardService.ozetGetir(1L, 9L, "Ali", "ADMIN", "BU_AY");
        DashboardOzetDTO buYil = dashboardService.ozetGetir(1L, 9L, "Ali", "ADMIN", "BU_YIL");

        assertEquals(0, buAy.getKpi().getCiro().getHedef().compareTo(BigDecimal.valueOf(10000)));
        assertEquals(0, buAy.getKpi().getCiro().getYuzde().compareTo(BigDecimal.ZERO));
        assertEquals(0, buAy.getKpi().getKar().getHedef().compareTo(BigDecimal.valueOf(2000)));
        assertNull(buYil.getKpi().getCiro().getHedef());
        assertNull(buYil.getKpi().getCiro().getYuzde());
        assertNull(buYil.getKpi().getKar().getHedef());
    }

    // ------------------------------------------------------------------
    // /vadeler
    // ------------------------------------------------------------------

    private VadeTakvimProjeksiyon projeksiyon(Long id, String numara, String cariAd, LocalDate vade,
                                              long genelToplam, long odenen) {
        return new VadeTakvimProjeksiyon() {
            @Override public Long getFaturaId() { return id; }
            @Override public String getFaturaNumarasi() { return numara; }
            @Override public String getCariAd() { return cariAd; }
            @Override public LocalDate getVadeTarihi() { return vade; }
            @Override public BigDecimal getGenelToplam() { return BigDecimal.valueOf(genelToplam); }
            @Override public BigDecimal getOdenenTutar() { return BigDecimal.valueOf(odenen); }
        };
    }

    @Test
    void vadeler_projeksiyonuDtoYaCevirirKalanTutarVeGecikmeBayragi() {
        LocalDate bugun = LocalDate.now();
        when(faturaRepository.vadeTakvimi(eq(1L), eq(Fatura.FaturaDurum.KESILDI), any(), any(), any(), any()))
                .thenReturn(List.of(
                        projeksiyon(10L, "F-10", "Cari A", bugun.minusDays(2), 1000, 400),
                        projeksiyon(11L, "F-11", "Cari B", bugun, 500, 500),
                        projeksiyon(12L, "F-12", "Cari C", bugun.plusDays(3), 300, 0)));
        LocalDate bas = bugun.minusDays(5);
        LocalDate bit = bugun.plusDays(10);

        List<DashboardVadeDTO> vadeler = dashboardService.vadelerGetir(1L, bas, bit);

        assertEquals(3, vadeler.size());
        assertEquals(10L, vadeler.get(0).getFaturaId());
        assertEquals("F-10", vadeler.get(0).getFaturaNumarasi());
        assertEquals("Cari A", vadeler.get(0).getCariAd());
        assertEquals(bugun.minusDays(2), vadeler.get(0).getVadeTarihi());
        assertEquals(0, vadeler.get(0).getKalanTutar().compareTo(BigDecimal.valueOf(600)));
        assertTrue(vadeler.get(0).getGeciktiMi());
        // Vadesi bugün olan fatura gecikmiş sayılmaz.
        assertFalse(vadeler.get(1).getGeciktiMi());
        assertFalse(vadeler.get(2).getGeciktiMi());

        verify(faturaRepository).vadeTakvimi(eq(1L), eq(Fatura.FaturaDurum.KESILDI),
                eq(List.of("ODENDI", "IPTAL")), eq(bas), eq(bit), eq(PageRequest.of(0, 200)));
    }

    @Test
    void vadeler_varsayilanAralikIcindeBulunulanAydir() {
        LocalDate bugun = LocalDate.now();
        when(faturaRepository.vadeTakvimi(eq(1L), any(), any(), any(), any(), any())).thenReturn(List.of());

        dashboardService.vadelerGetir(1L, null, null);

        verify(faturaRepository).vadeTakvimi(eq(1L), eq(Fatura.FaturaDurum.KESILDI), any(),
                eq(bugun.withDayOfMonth(1)),
                eq(bugun.withDayOfMonth(bugun.lengthOfMonth())),
                eq(PageRequest.of(0, 200)));
    }

    @Test
    void vadeler_tenantYoksaBusinessExceptionFirlatir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> dashboardService.vadelerGetir(null, LocalDate.now(), LocalDate.now().plusDays(1)));
        verifyNoInteractions(faturaRepository);
    }

    @Test
    void vadeler_baslangicBitisdenSonraysaBusinessExceptionFirlatir() {
        LocalDate bas = LocalDate.now();

        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> dashboardService.vadelerGetir(1L, bas, bas.minusDays(1)));
        verifyNoInteractions(faturaRepository);
    }

    @Test
    void vadeler_93GunSiniriniUygular() {
        LocalDate bas = LocalDate.now();
        when(faturaRepository.vadeTakvimi(eq(1L), any(), any(), any(), any(), any())).thenReturn(List.of());

        // 93 gün dahil kabul edilir.
        assertDoesNotThrow(() -> dashboardService.vadelerGetir(1L, bas, bas.plusDays(92)));
        // 94 gün reddedilir.
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> dashboardService.vadelerGetir(1L, bas, bas.plusDays(93)));
    }

    // ------------------------------------------------------------------
    // /kar-analizi
    // ------------------------------------------------------------------

    private KarAnalizSatirProjeksiyon karSatiri(Long id, String ad, BigDecimal ciro, BigDecimal maliyet) {
        return new KarAnalizSatirProjeksiyon() {
            @Override public Long getId() { return id; }
            @Override public String getAd() { return ad; }
            @Override public BigDecimal getCiro() { return ciro; }
            @Override public BigDecimal getMaliyet() { return maliyet; }
        };
    }

    private KarAnalizOzetProjeksiyon karOzeti(BigDecimal ciro, BigDecimal maliyet) {
        return new KarAnalizOzetProjeksiyon() {
            @Override public BigDecimal getCiro() { return ciro; }
            @Override public BigDecimal getMaliyet() { return maliyet; }
        };
    }

    @Test
    void karAnalizi_ozetKirilimlariVeMaliyetsizKalemSayisiniHesaplar() {
        when(faturaKalemRepository.karAnaliziOzet(eq(1L), eq(Fatura.FaturaTur.SATIS),
                eq(Fatura.FaturaDurum.KESILDI), any(), any()))
                .thenReturn(karOzeti(new BigDecimal("10000.00"), new BigDecimal("6000.00")));
        when(faturaKalemRepository.maliyetsizKalemSayisi(eq(1L), any(), any(), any(), any()))
                .thenReturn(3L);
        when(faturaKalemRepository.karAnaliziUrunler(eq(1L), any(), any(), any(), any(), any()))
                .thenReturn(List.of(karSatiri(11L, "Ürün A", new BigDecimal("1000.00"), new BigDecimal("400.00"))));
        when(faturaKalemRepository.karAnaliziCariler(eq(1L), any(), any(), any(), any(), any()))
                .thenReturn(List.of(karSatiri(21L, "Cari A", new BigDecimal("2000.00"), new BigDecimal("1500.00"))));

        DashboardKarAnaliziDTO sonuc = dashboardService.karAnaliziGetir(1L, "ADMIN", "BU_AY");

        assertEquals("BU_AY", sonuc.getDonem().getKod());
        assertEquals(0, sonuc.getOzet().getCiro().compareTo(new BigDecimal("10000.00")));
        assertEquals(0, sonuc.getOzet().getMaliyet().compareTo(new BigDecimal("6000.00")));
        assertEquals(0, sonuc.getOzet().getKar().compareTo(new BigDecimal("4000.00")));
        assertEquals(0, sonuc.getOzet().getMarj().compareTo(new BigDecimal("40.0")));
        assertEquals(3L, sonuc.getOzet().getMaliyetsizKalem());

        assertEquals(1, sonuc.getUrunler().size());
        var urun = sonuc.getUrunler().get(0);
        assertEquals(11L, urun.getStokId());
        assertEquals("Ürün A", urun.getAd());
        assertEquals(0, urun.getCiro().compareTo(new BigDecimal("1000.00")));
        assertEquals(0, urun.getMaliyet().compareTo(new BigDecimal("400.00")));
        assertEquals(0, urun.getKar().compareTo(new BigDecimal("600.00")));
        assertEquals(0, urun.getMarj().compareTo(new BigDecimal("60.0")));

        assertEquals(1, sonuc.getCariler().size());
        var cari = sonuc.getCariler().get(0);
        assertEquals(21L, cari.getCariId());
        assertEquals("Cari A", cari.getAd());
        assertEquals(0, cari.getKar().compareTo(new BigDecimal("500.00")));
        assertEquals(0, cari.getMarj().compareTo(new BigDecimal("25.0")));
    }

    @Test
    void karAnalizi_dortDonemAraliginiKullanir() {
        LocalDate bugun = LocalDate.now();
        LocalDate ayBas = bugun.withDayOfMonth(1);
        LocalDate aySon = bugun.withDayOfMonth(bugun.lengthOfMonth());
        LocalDate gecenAyBas = bugun.minusMonths(1).withDayOfMonth(1);
        LocalDate gecenAySon = gecenAyBas.withDayOfMonth(gecenAyBas.lengthOfMonth());
        int ilkAy = ((bugun.getMonthValue() - 1) / 3) * 3 + 1;
        LocalDate ceyrekBas = LocalDate.of(bugun.getYear(), ilkAy, 1);
        LocalDate ceyrekSon = ceyrekBas.plusMonths(3).minusDays(1);
        LocalDate yilBas = LocalDate.of(bugun.getYear(), 1, 1);
        LocalDate yilSon = LocalDate.of(bugun.getYear(), 12, 31);

        var basCap = org.mockito.ArgumentCaptor.forClass(LocalDate.class);
        var bitCap = org.mockito.ArgumentCaptor.forClass(LocalDate.class);

        var buAy = dashboardService.karAnaliziGetir(1L, "USER", "BU_AY");
        var gecenAy = dashboardService.karAnaliziGetir(1L, "USER", "gecen_ay");
        var buCeyrek = dashboardService.karAnaliziGetir(1L, "USER", "BU_CEYREK");
        var buYil = dashboardService.karAnaliziGetir(1L, "USER", "BU_YIL");

        verify(faturaKalemRepository, times(4)).karAnaliziOzet(eq(1L),
                eq(Fatura.FaturaTur.SATIS), eq(Fatura.FaturaDurum.KESILDI),
                basCap.capture(), bitCap.capture());
        assertEquals(List.of(ayBas, gecenAyBas, ceyrekBas, yilBas), basCap.getAllValues());
        assertEquals(List.of(aySon, gecenAySon, ceyrekSon, yilSon), bitCap.getAllValues());

        assertEquals("Bu Ay", buAy.getDonem().getEtiket());
        assertEquals("GECEN_AY", gecenAy.getDonem().getKod());
        assertEquals("Geçen Ay", gecenAy.getDonem().getEtiket());
        assertEquals("Bu Çeyrek", buCeyrek.getDonem().getEtiket());
        assertEquals("Bu Yıl", buYil.getDonem().getEtiket());
    }

    @Test
    void karAnalizi_urunVeCariIcinEnFazlaOnSatirIster() {
        dashboardService.karAnaliziGetir(1L, "USER", "BU_AY");

        verify(faturaKalemRepository).karAnaliziUrunler(eq(1L), any(), any(), any(), any(),
                eq(PageRequest.of(0, 10)));
        verify(faturaKalemRepository).karAnaliziCariler(eq(1L), any(), any(), any(), any(),
                eq(PageRequest.of(0, 10)));
    }

    @Test
    void karAnalizi_bosVerideSifirlarVeNullMarjDoner() {
        DashboardKarAnaliziDTO sonuc = dashboardService.karAnaliziGetir(1L, "USER", "BU_AY");

        assertEquals(0, sonuc.getOzet().getCiro().compareTo(BigDecimal.ZERO));
        assertEquals(0, sonuc.getOzet().getMaliyet().compareTo(BigDecimal.ZERO));
        assertEquals(0, sonuc.getOzet().getKar().compareTo(BigDecimal.ZERO));
        assertNull(sonuc.getOzet().getMarj());
        assertEquals(0L, sonuc.getOzet().getMaliyetsizKalem());
        assertTrue(sonuc.getUrunler().isEmpty());
        assertTrue(sonuc.getCariler().isEmpty());
    }

    @Test
    void karAnalizi_ciroSifirsaMarjNullDoner() {
        when(faturaKalemRepository.karAnaliziOzet(eq(1L), any(), any(), any(), any()))
                .thenReturn(karOzeti(BigDecimal.ZERO, BigDecimal.ZERO));
        when(faturaKalemRepository.karAnaliziUrunler(eq(1L), any(), any(), any(), any(), any()))
                .thenReturn(List.of(karSatiri(11L, "Bedelsiz", BigDecimal.ZERO, BigDecimal.ZERO)));

        DashboardKarAnaliziDTO sonuc = dashboardService.karAnaliziGetir(1L, "USER", "BU_AY");

        assertEquals(0, sonuc.getOzet().getKar().compareTo(BigDecimal.ZERO));
        assertNull(sonuc.getOzet().getMarj());
        assertNull(sonuc.getUrunler().get(0).getMarj());
    }

    @Test
    void karAnalizi_maliyetNullsaSifirKabulEdilir() {
        when(faturaKalemRepository.karAnaliziUrunler(eq(1L), any(), any(), any(), any(), any()))
                .thenReturn(List.of(karSatiri(11L, "Maliyetsiz", new BigDecimal("500.00"), null)));
        when(faturaKalemRepository.karAnaliziCariler(eq(1L), any(), any(), any(), any(), any()))
                .thenReturn(List.of(karSatiri(21L, "Cari", null, null)));

        DashboardKarAnaliziDTO sonuc = dashboardService.karAnaliziGetir(1L, "USER", "BU_AY");

        var urun = sonuc.getUrunler().get(0);
        assertEquals(0, urun.getMaliyet().compareTo(BigDecimal.ZERO));
        assertEquals(0, urun.getKar().compareTo(new BigDecimal("500.00")));
        assertEquals(0, urun.getMarj().compareTo(new BigDecimal("100.0")));

        var cari = sonuc.getCariler().get(0);
        assertEquals(0, cari.getCiro().compareTo(BigDecimal.ZERO));
        assertEquals(0, cari.getKar().compareTo(BigDecimal.ZERO));
        assertNull(cari.getMarj());
    }

    @Test
    void karAnalizi_adsizUrunVeCariIcinVarsayilanEtiketKullanir() {
        when(faturaKalemRepository.karAnaliziUrunler(eq(1L), any(), any(), any(), any(), any()))
                .thenReturn(List.of(karSatiri(11L, null, BigDecimal.TEN, BigDecimal.ONE),
                        karSatiri(12L, "  ", BigDecimal.TEN, BigDecimal.ONE)));
        when(faturaKalemRepository.karAnaliziCariler(eq(1L), any(), any(), any(), any(), any()))
                .thenReturn(List.of(karSatiri(21L, null, BigDecimal.TEN, BigDecimal.ONE)));

        DashboardKarAnaliziDTO sonuc = dashboardService.karAnaliziGetir(1L, "USER", "BU_AY");

        assertEquals("(Ürün adı yok)", sonuc.getUrunler().get(0).getAd());
        assertEquals("(Ürün adı yok)", sonuc.getUrunler().get(1).getAd());
        assertEquals("(Cari adı yok)", sonuc.getCariler().get(0).getAd());
    }

    @Test
    void karAnalizi_repoSirasiniKorur() {
        when(faturaKalemRepository.karAnaliziUrunler(eq(1L), any(), any(), any(), any(), any()))
                .thenReturn(List.of(
                        karSatiri(1L, "En kârlı", new BigDecimal("1000"), new BigDecimal("100")),
                        karSatiri(2L, "İkinci", new BigDecimal("1000"), new BigDecimal("500")),
                        karSatiri(3L, "Üçüncü", new BigDecimal("1000"), new BigDecimal("900"))));

        DashboardKarAnaliziDTO sonuc = dashboardService.karAnaliziGetir(1L, "USER", "BU_AY");

        assertEquals(List.of("En kârlı", "İkinci", "Üçüncü"),
                sonuc.getUrunler().stream().map(DashboardKarAnaliziDTO.UrunDTO::getAd).toList());
        assertEquals(900, sonuc.getUrunler().get(0).getKar().intValue());
        assertEquals(100, sonuc.getUrunler().get(2).getKar().intValue());
    }

    @Test
    void karAnalizi_tenantYoksaBusinessExceptionFirlatir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> dashboardService.karAnaliziGetir(null, "USER", "BU_AY"));
        verifyNoInteractions(faturaKalemRepository);
    }

    @Test
    void karAnalizi_gecersizDonemBusinessExceptionFirlatir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> dashboardService.karAnaliziGetir(1L, "USER", "DUN"));
        verifyNoInteractions(faturaKalemRepository);
    }
}
