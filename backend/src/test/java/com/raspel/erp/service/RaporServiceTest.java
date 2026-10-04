package com.raspel.erp.service;

import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.dto.sistem.RaporDTO;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.entity.finans.Hareket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.service.sistem.RaporService;

@ExtendWith(MockitoExtension.class)
class RaporServiceTest {

    @Mock private CariHesapRepository cariHesapRepository;
    @Mock private HareketRepository hareketRepository;
    @Mock private FaturaRepository faturaRepository;
    @Mock private com.raspel.erp.repository.ticaret.FaturaKalemRepository faturaKalemRepository;
    @Mock private com.raspel.erp.repository.envanter.StokRepository stokRepository;
    @Mock private com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    @Mock private com.raspel.erp.repository.finans.BankaRepository bankaRepository;
    @Mock private CariHesapService cariHesapService;
    @Mock private HareketService hareketService;
    @Mock private com.raspel.erp.repository.finans.ButceRepository butceRepository;
    @Mock private com.raspel.erp.repository.finans.MasrafRepository masrafRepository;
    @Mock private com.raspel.erp.service.sistem.PdfRaporService pdfRaporService;
    @Mock private TenantChecker tenantChecker;
    @Mock private com.raspel.erp.service.envanter.MaliyetService maliyetService;
    @Mock private com.raspel.erp.repository.ticaret.IadeRepository iadeRepository;
    @Mock private com.raspel.erp.repository.ticaret.IadeKalemRepository iadeKalemRepository;
    @Mock private com.raspel.erp.repository.envanter.StokMaliyetHareketRepository stokMaliyetHareketRepository;
    @InjectMocks private RaporService raporService;

    private CariHesap createCariHesap() {
        CariHesap c = new CariHesap();
        c.setId(1L);
        c.setAd("Test Cari");
        c.setBakiye(BigDecimal.valueOf(5000));
        c.setOlusturmaTarihi(LocalDateTime.now());
        c.setGuncellemeTarihi(LocalDateTime.now());
        return c;
    }

    private Hareket createHareket() {
        CariHesap cari = createCariHesap();
        Hareket h = new Hareket();
        h.setId(1L);
        h.setCariHesap(cari);
        h.setTur(Hareket.HareketTuru.TAHSILAT);
        h.setTutar(BigDecimal.valueOf(1000));
        h.setHareketTarihi(LocalDate.now());
        h.setOlusturmaTarihi(LocalDateTime.now());
        return h;
    }

    @Test
    void cariEkstreGetir_returnsEkstre() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findById(1L)).thenReturn(java.util.Optional.of(cari));
        Hareket hareket = createHareket();
        when(hareketRepository.findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(any(), any(), any()))
                .thenReturn(List.of(hareket));
        var result = raporService.cariEkstreGetir(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals("Test Cari", result.getCariAd());
    }

    @Test
    void cariEkstreGetir_throwsWhenCariNotFound() {
        when(cariHesapRepository.findById(99L)).thenReturn(java.util.Optional.empty());
        assertThrows(RuntimeException.class, () -> raporService.cariEkstreGetir(99L, LocalDate.now(), LocalDate.now()));
    }

    @Test
    void cariEkstreGetir_faturalariDaEkler() {
        CariHesap cari = createCariHesap();
        cari.setBakiye(new BigDecimal("-500"));
        when(cariHesapRepository.findById(1L)).thenReturn(java.util.Optional.of(cari));
        when(hareketRepository.findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(any(), any(), any()))
                .thenReturn(List.of());
        com.raspel.erp.entity.ticaret.Fatura f = com.raspel.erp.entity.ticaret.Fatura.builder()
                .id(7L).tur(com.raspel.erp.entity.ticaret.Fatura.FaturaTur.SATIS)
                .genelToplam(new BigDecimal("500")).tarih(LocalDate.of(2026, 6, 1))
                .faturaNumarasi("F-1").cariHesap(cari).build();
        when(faturaRepository.findByCariHesapIdAndDurumAndTarihBetweenOrderByTarihAscIdAsc(any(), any(), any(), any()))
                .thenReturn(List.of(f));

        var result = raporService.cariEkstreGetir(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertEquals(1, result.getHareketler().size());
        assertEquals("SATIS_FATURA", result.getHareketler().get(0).getTur());
        // Guncel bakiye -500, donem etkisi -500 => donem basi 0
        assertEquals(0, result.getDonemBasBakiye().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.getDonemSonBakiye().compareTo(new BigDecimal("-500")));
    }

    @Test
    void gelirGiderOzeti_returnsOzet() {
        Hareket tahsilat = createHareket();
        Hareket odeme = createHareket();
        odeme.setTur(Hareket.HareketTuru.ODEME);
        when(hareketRepository.findBySirketIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(any(), any(), any()))
                .thenReturn(List.of(tahsilat, odeme));
        var result = raporService.gelirGiderOzeti(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 1L);
        assertEquals(BigDecimal.valueOf(1000), result.getToplamGelir());
    }

    @Test
    void kdvRaporu_returnsKdv() {
        Fatura fatura = new Fatura();
        fatura.setTur(Fatura.FaturaTur.SATIS);
        fatura.setDurum(Fatura.FaturaDurum.KESILDI);
        fatura.setKdv(BigDecimal.valueOf(200));
        fatura.setTarih(LocalDate.now());
        Fatura faturaAlis = new Fatura();
        faturaAlis.setTur(Fatura.FaturaTur.ALIS);
        faturaAlis.setDurum(Fatura.FaturaDurum.KESILDI);
        faturaAlis.setKdv(BigDecimal.valueOf(100));
        faturaAlis.setTarih(LocalDate.now());
        when(faturaRepository.basliklariTarihAraligindaGetir(eq(1L), any(), any())).thenReturn(List.of(fatura, faturaAlis));
        var result = raporService.kdvRaporu(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 1L);
        assertEquals(BigDecimal.valueOf(200), result.getToplamKdvCikis());
        assertEquals(BigDecimal.valueOf(100), result.getToplamKdvGiris());
        assertEquals(BigDecimal.valueOf(100), result.getKdvFarki());
    }

    @Test
    void yaslandirmaRaporu_kovaMatrisiDoner() {
        CariHesap cari1 = createCariHesap();
        cari1.setAd("Cari 1");
        CariHesap cari2 = createCariHesap();
        cari2.setId(2L);
        cari2.setAd("Cari 2");

        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(cari1, cari2));
        // Satır: [cariId, vadeTarihi, kalanTutar] - kovalama Java tarafinda yapilir.
        LocalDate bugun = LocalDate.now();
        when(faturaRepository.acikFaturalarVadeIcin(any(), any(), any(), anyList()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{1L, bugun.plusDays(10), new BigDecimal("2000")},   // vadesi gelmemis
                        new Object[]{1L, bugun.minusDays(45), new BigDecimal("1000")},   // 31-60
                        new Object[]{1L, bugun.minusDays(120), new BigDecimal("500")},  // 90+
                        new Object[]{2L, bugun.minusDays(10), new BigDecimal("700")}));  // 0-30

        var rapor = raporService.yaslandirmaRaporu(1L, null);

        assertEquals(2, rapor.getSatirlar().size());
        // En cok geciken cari en ustte.
        var ilk = rapor.getSatirlar().get(0);
        assertEquals(1L, ilk.getCariHesapId());
        assertEquals("Cari 1", ilk.getCariAd());
        assertEquals(120, ilk.getEnFazlaGecikmeGun());
        // Ortalama yalnizca gecmis iki fatura uzerinden: (45 + 120) / 2
        assertEquals(82.5d, ilk.getOrtalamaGecikmeGun(), 0.001d);
        // Satir toplami kova toplamlarinin toplami olmali.
        assertEquals(0, ilk.getToplam().compareTo(new BigDecimal("3500")));
        // Gecmis tutar "vadesi gelmemis" haric kova toplami.
        assertEquals(0, ilk.getGecikmisTutar().compareTo(new BigDecimal("1500")));
        assertEquals(new BigDecimal("2000"), ilk.getKovalar().get("VADEDI_GELMEMIS"));
        assertEquals(BigDecimal.ZERO, ilk.getKovalar().get("GUN_0_30"), "Doluluk yoksa sifir yazilmali");
        assertEquals(new BigDecimal("500"), ilk.getKovalar().get("GUN_90_PLUS"));

        var ozet = rapor.getOzet();
        assertEquals(2, ozet.getCariSayisi());
        assertEquals(0, ozet.getToplam().compareTo(new BigDecimal("4200")));
        assertEquals(0, ozet.getGecikmisTutar().compareTo(new BigDecimal("2200")));
        assertEquals(new BigDecimal("2000"), ozet.getKovalar().get("VADEDI_GELMEMIS"));
        // Gecmis kovalarin toplami: 700 + 1000 + 0 + 500 = 2200
        assertEquals(new BigDecimal("2200"), ozet.getKovalar().get("GUN_0_30")
                .add(ozet.getKovalar().get("GUN_31_60")).add(ozet.getKovalar().get("GUN_61_90"))
                .add(ozet.getKovalar().get("GUN_90_PLUS")));
        assertEquals(RaporService.YASLANDIRMA_KOVALARI, ozet.getKovaSirasi());
    }

    /**
     * Regresyon: satir tutari carinin NET bakiyesinin mutlak degeri olarak
     * hesaplaniyordu. 1.000 TL vadesi gecmis fatura + 500 TL pesin tahsilati olan
     * cari 90+ kovasinda yalnizca 500 TL ile gorunuyor, yani rapor tahsil
     * edilebilecek tutari oldugundan az gosteriyordu.
     */
    @Test
    void yaslandirmaRaporu_netBakiyeDegilKalanTutarToplar() {
        CariHesap cari = createCariHesap();
        cari.setAd("Kisisel");
        // Net bakiye -1500 (1.000 gecmis borc - 500 pesin tahsilat).
        cari.setBakiye(BigDecimal.valueOf(-1500));

        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(cari));
        when(faturaRepository.acikFaturalarVadeIcin(any(), any(), any(), anyList()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{1L, LocalDate.now().minusDays(120), new BigDecimal("1000")}));

        var rapor = raporService.yaslandirmaRaporu(1L, null);

        var satir = rapor.getSatirlar().get(0);
        assertEquals(0, satir.getToplam().compareTo(new BigDecimal("1000")),
                "Satir toplami gecmis faturanin kalan tutari olmali, net bakiye degil");
        assertEquals(0, satir.getGecikmisTutar().compareTo(new BigDecimal("1000")));
    }

    /** Vadesi null olan fatura gecmis sayilmaz; mutabakat icin ilk kovaya girer. */
    @Test
    void yaslandirmaRaporu_vadesiNullOlanFaturaVadesiGelmemisKovasinaGirer() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(cari));
        when(faturaRepository.acikFaturalarVadeIcin(any(), any(), any(), anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, null, new BigDecimal("750")}));

        var satir = raporService.yaslandirmaRaporu(1L, null).getSatirlar().get(0);

        assertEquals(new BigDecimal("750"), satir.getKovalar().get("VADEDI_GELMEMIS"));
        assertEquals(0, satir.getGecikmisTutar().compareTo(BigDecimal.ZERO));
        assertEquals(0, satir.getEnFazlaGecikmeGun());
        assertEquals(0d, satir.getOrtalamaGecikmeGun(), 0.001d);
    }

    /** Ayni fatura farkli referans tarihlerinde farkli kovalara duser. */
    @Test
    void yaslandirmaRaporu_referansTariheGoreKovalar() {
        CariHesap cari = createCariHesap();
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of(cari));
        when(faturaRepository.acikFaturalarVadeIcin(any(), any(), any(), anyList()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{1L, LocalDate.of(2026, 1, 1), new BigDecimal("400")}));

        // 1 Ocak vadeli fatura 15 Subat referansinda 45 gun gecmis -> 31-60
        var satir = raporService.yaslandirmaRaporu(1L, LocalDate.of(2026, 2, 15)).getSatirlar().get(0);
        assertEquals(45, satir.getEnFazlaGecikmeGun());
        assertEquals(new BigDecimal("400"), satir.getKovalar().get("GUN_31_60"));
        assertEquals(BigDecimal.ZERO, satir.getKovalar().get("VADEDI_GELMEMIS"));

        // 1 Mart referansinda ayni fatura 59 gun gecmis -> hala 31-60
        assertEquals(59, raporService.yaslandirmaRaporu(1L, LocalDate.of(2026, 3, 1))
                .getSatirlar().get(0).getEnFazlaGecikmeGun());
        // 1 Nisan referansinda 90 gun -> 61-90
        assertEquals(new BigDecimal("400"), raporService.yaslandirmaRaporu(1L, LocalDate.of(2026, 4, 1))
                .getSatirlar().get(0).getKovalar().get("GUN_61_90"));
    }

    @Test
    void yaslandirmaRaporu_veriYoksaBosOzetDoner() {
        when(cariHesapRepository.findBySirketIdOrderByAdAsc(1L)).thenReturn(List.of());
        when(faturaRepository.acikFaturalarVadeIcin(any(), any(), any(), anyList()))
                .thenReturn(List.of());

        var rapor = raporService.yaslandirmaRaporu(1L, null);

        assertTrue(rapor.getSatirlar().isEmpty());
        assertEquals(0, rapor.getOzet().getToplam().compareTo(BigDecimal.ZERO));
        assertEquals(0, rapor.getOzet().getCariSayisi());
        // Tum kovalar sifir olmali; eksik anahtar frontend'de "undefined" gorunur.
        assertEquals(RaporService.YASLANDIRMA_KOVALARI.size(), rapor.getOzet().getKovalar().size());
        assertTrue(rapor.getOzet().getKovalar().values().stream().allMatch(v -> v.signum() == 0));
    }

    @Test
    void kdvBeyannameGetir_hesaplananVeIndirilecekKdv() {
        LocalDate ayIci = LocalDate.of(2026, 7, 15);
        Fatura satis = new Fatura();
        satis.setId(1L);
        satis.setTur(Fatura.FaturaTur.SATIS);
        satis.setDurum(Fatura.FaturaDurum.KESILDI);
        satis.setTarih(ayIci);
        satis.setKalemler(List.of(FaturaKalem.builder().kdvOrani(new BigDecimal("18")).tutar(BigDecimal.valueOf(1180)).build()));
        Fatura alis = new Fatura();
        alis.setId(2L);
        alis.setTur(Fatura.FaturaTur.ALIS);
        alis.setDurum(Fatura.FaturaDurum.KESILDI);
        alis.setTarih(ayIci);
        alis.setKalemler(List.of(FaturaKalem.builder().kdvOrani(new BigDecimal("20")).tutar(BigDecimal.valueOf(1200)).build()));

        when(faturaRepository.findBySirketIdAndTarihBetweenKalemli(eq(1L), any(), any())).thenReturn(List.of(satis, alis));

        var result = raporService.kdvBeyannameGetir("2026-07", 1L);

        assertEquals(0, BigDecimal.valueOf(180).compareTo(result.getToplamHesaplananKdv()));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(result.getToplamIndirilecekKdv()));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(result.getDevredenKdv()));
    }

    @Test
    void baBsGetir_esiginUzerindekiKayitlariListeler() {
        LocalDate ayIci = LocalDate.of(2026, 7, 10);
        Fatura buyuk = new Fatura();
        buyuk.setId(1L);
        buyuk.setFaturaNumarasi("FTR-1");
        buyuk.setTur(Fatura.FaturaTur.SATIS);
        buyuk.setDurum(Fatura.FaturaDurum.KESILDI);
        buyuk.setTarih(ayIci);
        buyuk.setAraToplam(BigDecimal.valueOf(8000));
        buyuk.setKdv(BigDecimal.valueOf(1440));
        buyuk.setGenelToplam(BigDecimal.valueOf(9440));
        Fatura kucuk = new Fatura();
        kucuk.setId(2L);
        kucuk.setFaturaNumarasi("FTR-2");
        kucuk.setTur(Fatura.FaturaTur.SATIS);
        kucuk.setDurum(Fatura.FaturaDurum.KESILDI);
        kucuk.setTarih(ayIci);
        kucuk.setGenelToplam(BigDecimal.valueOf(1000));

        when(faturaRepository.basliklariTarihAraligindaGetir(eq(1L), any(), any())).thenReturn(List.of(buyuk, kucuk));

        var result = raporService.baBsGetir("2026-07", "BS", new BigDecimal("5000"), 1L);

        assertEquals("BS", result.getTur());
        assertEquals(1, result.getKayitlar().size());
        assertEquals("FTR-1", result.getKayitlar().get(0).getFaturaNo());
        assertEquals(BigDecimal.valueOf(9440), result.getToplamTutar());
    }

    @Test
    void tedarikciUrunRaporu_returnsGrouped() {
        when(faturaKalemRepository.tedarikciUrunler(eq(1L), eq(Fatura.FaturaTur.ALIS), eq(Fatura.FaturaDurum.KESILDI)))
                .thenReturn(List.of(new com.raspel.erp.repository.ticaret.TedarikciUrunProjeksiyon() {
                    public Long getCariHesapId() { return 1L; }
                    public String getCariHesapAd() { return "ABC Fabrika"; }
                    public Long getStokId() { return 10L; }
                    public Long getToplamMiktar() { return 50L; }
                    public BigDecimal getSonBirimFiyat() { return BigDecimal.valueOf(120); }
                    public LocalDate getSonTarih() { return LocalDate.of(2026, 7, 1); }
                }));

        com.raspel.erp.entity.envanter.Stok stok = new com.raspel.erp.entity.envanter.Stok();
        stok.setId(10L);
        stok.setAd("MDF 18mm");
        stok.setStokKodu("MDF-18");
        when(stokRepository.findAllById(java.util.List.of(10L))).thenReturn(java.util.List.of(stok));

        var result = raporService.tedarikciUrunRaporu(1L);

        assertEquals(1, result.size());
        assertEquals("ABC Fabrika", result.get(0).getCariHesapAd());
        assertEquals("MDF 18mm", result.get(0).getStokAd());
        assertEquals(50L, result.get(0).getToplamMiktar());
    }

    @Test
    void urunKarlilikRaporu_returnsMargin() {
        com.raspel.erp.entity.envanter.Stok stok = new com.raspel.erp.entity.envanter.Stok();
        stok.setId(1L);
        stok.setAd("MDF 18mm");
        stok.setStokKodu("MDF-18");
        stok.setFiyat(BigDecimal.valueOf(100));
        stok.setSatisFiyati(BigDecimal.valueOf(150));
        when(stokRepository.findBySirketIdOrderByAd(1L)).thenReturn(java.util.List.of(stok));

        var result = raporService.urunKarlilikRaporu(1L);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getKar().compareTo(BigDecimal.valueOf(50)));
        assertEquals(0, result.get(0).getKarMarji().compareTo(new BigDecimal("33.33")));
    }

    @Test
    void nakitAkisiProjeksiyonu_calculatesDailyAndCumulative() {
        com.raspel.erp.entity.finans.Kasa k = new com.raspel.erp.entity.finans.Kasa();
        k.setBakiye(BigDecimal.valueOf(10000));
        when(kasaRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(k));

        com.raspel.erp.entity.finans.Banka b = new com.raspel.erp.entity.finans.Banka();
        b.setBakiye(BigDecimal.valueOf(40000));
        when(bankaRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(b));

        Fatura fSatis = new Fatura();
        fSatis.setTur(Fatura.FaturaTur.SATIS);
        fSatis.setDurum(Fatura.FaturaDurum.KESILDI);
        fSatis.setGenelToplam(BigDecimal.valueOf(15000));
        fSatis.setKalanTutar(BigDecimal.valueOf(15000));
        fSatis.setVadeTarihi(LocalDate.now().plusDays(5));

        Fatura fAlis = new Fatura();
        fAlis.setTur(Fatura.FaturaTur.ALIS);
        fAlis.setDurum(Fatura.FaturaDurum.KESILDI);
        fAlis.setGenelToplam(BigDecimal.valueOf(5000));
        fAlis.setKalanTutar(BigDecimal.valueOf(5000));
        fAlis.setVadeTarihi(LocalDate.now().plusDays(10));

        when(faturaRepository.basliklariGetir(1L)).thenReturn(List.of(fSatis, fAlis));

        var result = raporService.nakitAkisiProjeksiyonu(30, 1L);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(50000), result.getBaslangicBakiyesi());
        assertEquals(BigDecimal.valueOf(15000), result.getToplamBeklenenGiris());
        assertEquals(BigDecimal.valueOf(5000), result.getToplamBeklenenCikis());
        assertEquals(BigDecimal.valueOf(60000), result.getTahminiBitisBakiyesi());
        assertEquals(31, result.getGunlukAkis().size());
    }

    @Test
    void butceGerceklesen_kategoriBazliKarsilastirir() {
        com.raspel.erp.entity.finans.Butce butce = com.raspel.erp.entity.finans.Butce.builder()
                .yil(2026).ay(9).kategori("Pazarlama").tutar(BigDecimal.valueOf(1000)).sirketId(1L).build();
        com.raspel.erp.entity.finans.Masraf masraf = com.raspel.erp.entity.finans.Masraf.builder()
                .tarih(LocalDate.of(2026, 9, 10)).kategori("Pazarlama").tutar(BigDecimal.valueOf(600)).sirketId(1L).build();

        when(butceRepository.findBySirketIdOrderByYilDescAyDesc(eq(1L), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(butce)));
        when(masrafRepository.findBySirketIdAndTarihBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(masraf));

        var result = raporService.butceGerceklesenRaporu(1L, 2026, 9);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Pazarlama", result.get(0).getKategori());
        assertEquals(BigDecimal.valueOf(1000), result.get(0).getButce());
        assertEquals(BigDecimal.valueOf(600), result.get(0).getGerceklesen());
        assertEquals(BigDecimal.valueOf(-400), result.get(0).getSapma());
        assertNotNull(result.get(0).getKullanimYuzdesi());
    }

    @Test
    void stokDegerleme_fifoKatmanlariDogruTuketir() {
        com.raspel.erp.entity.envanter.Stok stok = com.raspel.erp.entity.envanter.Stok.builder()
                .id(1L).stokKodu("STK-1").ad("Ürün").miktar(new BigDecimal("5")).sirketId(1L).build();
        when(stokRepository.findBySirketIdOrderByAd(1L)).thenReturn(List.of(stok));
        when(maliyetService.ortalamaMaliyet(stok)).thenReturn(new BigDecimal("6"));
        when(stokMaliyetHareketRepository.findByStokIdOrderByTarihAscIdAsc(1L)).thenReturn(List.of(
                com.raspel.erp.entity.envanter.StokMaliyetHareket.builder()
                        .tur("GIRIS").miktar(new BigDecimal("10")).birimMaliyet(new BigDecimal("5")).build(),
                com.raspel.erp.entity.envanter.StokMaliyetHareket.builder()
                        .tur("GIRIS").miktar(new BigDecimal("10")).birimMaliyet(new BigDecimal("7")).build(),
                com.raspel.erp.entity.envanter.StokMaliyetHareket.builder()
                        .tur("CIKIS").miktar(new BigDecimal("15")).build()));

        var sonuc = raporService.stokDegerleme(1L);

        assertEquals(1, sonuc.getKalemSayisi());
        // Ağırlıklı ortalama: 5 x 6 = 30; FIFO: ilk katman tükendi, kalan 5 x 7 = 35.
        assertEquals(0, sonuc.getToplamOrtalamaDeger().compareTo(new BigDecimal("30.00")));
        assertEquals(0, sonuc.getToplamFifoDeger().compareTo(new BigDecimal("35.00")));
        assertEquals(0, sonuc.getSatirlar().get(0).getFifoBirimMaliyet().compareTo(new BigDecimal("7.00")));
    }

    @Test
    void siparisOnerisi_hedefeTamamlar() {
        com.raspel.erp.entity.envanter.Stok stok = com.raspel.erp.entity.envanter.Stok.builder()
                .id(1L).stokKodu("STK-1").ad("Ürün")
                .miktar(new BigDecimal("2")).minMiktar(new BigDecimal("5")).sirketId(1L).build();
        when(stokRepository.kritikStoklar(1L)).thenReturn(List.of(stok));
        when(maliyetService.ortalamaMaliyet(stok)).thenReturn(new BigDecimal("10"));

        var sonuc = raporService.siparisOnerisi(1L);

        assertEquals(1, sonuc.size());
        // Hedef = 5 x 2 = 10; öneri = 10 - 2 = 8; tahmini tutar = 80.
        assertEquals(0, sonuc.get(0).getOneriMiktar().compareTo(new BigDecimal("8")));
        assertEquals(0, sonuc.get(0).getTahminiTutar().compareTo(new BigDecimal("80.00")));
    }

    @Test
    void temsilciPerformans_toplamlariHesaplar() {
        com.raspel.erp.repository.ticaret.TemsilciPerformansProjeksiyon p1 =
                org.mockito.Mockito.mock(com.raspel.erp.repository.ticaret.TemsilciPerformansProjeksiyon.class);
        when(p1.getTemsilciId()).thenReturn(3L);
        when(p1.getTemsilciAd()).thenReturn("Ali");
        when(p1.getFaturaSayisi()).thenReturn(2L);
        when(p1.getToplamSatis()).thenReturn(new BigDecimal("1000"));
        when(faturaRepository.temsilciPerformans(eq(1L), any(), any(), any(), any()))
                .thenReturn(List.of(p1));

        var sonuc = raporService.temsilciPerformans(1L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertEquals(1, sonuc.getSatirlar().size());
        assertEquals(0, sonuc.getToplamSatis().compareTo(new BigDecimal("1000")));
        assertEquals(2, sonuc.getToplamFatura());
        assertEquals(0, sonuc.getSatirlar().get(0).getOrtalamaFatura().compareTo(new BigDecimal("500.00")));
    }

    // N+1 regresyonu: iade kalemleri ve iade bagli faturalarin cari bilgileri
    // iade basina ayri sorguyla (findByIadeId / findById) cekiliyordu; toplu
    // cagrilarla sabit sayida sorguya indirilir.
    @Test
    void baBsGetir_iadeKalemVeCariBilgisiniTopluCeker() {
        when(faturaRepository.basliklariTarihAraligindaGetir(any(), any(), any())).thenReturn(List.of());
        when(iadeRepository.findBySirketIdAndTurAndDurumAndTarihBetween(
                eq(1L), eq("SATIS"), eq("TAMAMLANDI"), any(), any()))
                .thenReturn(List.of(iade(1L, 900L), iade(2L, 900L), iade(3L, null)));

        when(iadeKalemRepository.findByIadeIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(
                iadeKalem(11L, 1L, "100", "20"),
                iadeKalem(12L, 2L, "200", "20"),
                iadeKalem(13L, 3L, "50", "20")));

        CariHesap cari = new CariHesap();
        cari.setId(7L);
        cari.setAd("ABC Ltd");
        cari.setVergiNumarasi("1234567890");
        when(faturaRepository.findAllById(List.of(900L))).thenReturn(List.of(
                Fatura.builder().id(900L).cariHesap(cari).build()));
        when(cariHesapRepository.findAllById(Set.of(7L))).thenReturn(List.of(cari));

        var sonuc = raporService.baBsGetir("2026-01", "BS", new BigDecimal("5000"), 1L);

        assertEquals(3, sonuc.getKayitlar().size());
        var ilk = sonuc.getKayitlar().stream()
                .filter(k -> k.getFaturaNo().equals("İADE #1")).findFirst().orElseThrow();
        assertEquals("ABC Ltd", ilk.getCariAd());
        assertEquals("1234567890", ilk.getCariVkn());
        // birimFiyat KDV dahil: net = 100 / 1.20 = 83.33, kdv = 100 - 83.33 = 16.67
        assertEquals(0, ilk.getMatrah().compareTo(new BigDecimal("-83.33")));
        assertEquals(0, ilk.getKdv().compareTo(new BigDecimal("-16.67")));
        assertEquals(0, ilk.getTutar().compareTo(new BigDecimal("-6000")));

        var iadesiz = sonuc.getKayitlar().stream()
                .filter(k -> k.getFaturaNo().equals("İADE #3")).findFirst().orElseThrow();
        assertNull(iadesiz.getCariAd());

        verify(iadeKalemRepository, times(1)).findByIadeIdIn(List.of(1L, 2L, 3L));
        verify(iadeKalemRepository, never()).findByIadeId(any());
        verify(faturaRepository, times(1)).findAllById(List.of(900L));
        verify(faturaRepository, never()).findById(any());
        verify(cariHesapRepository, times(1)).findAllById(Set.of(7L));
    }

    @Test
    void baBsGetir_iadeYoksaTopluSorguCagirmaz() {
        when(faturaRepository.basliklariTarihAraligindaGetir(any(), any(), any())).thenReturn(List.of());
        when(iadeRepository.findBySirketIdAndTurAndDurumAndTarihBetween(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        var sonuc = raporService.baBsGetir("2026-01", "BA", null, 1L);

        assertTrue(sonuc.getKayitlar().isEmpty());
        assertEquals(0, sonuc.getToplamTutar().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("5000"), sonuc.getEsik());
        verify(iadeKalemRepository, never()).findByIadeIdIn(any());
        verify(faturaRepository, never()).findAllById(any());
        verify(cariHesapRepository, never()).findAllById(any());
    }

    @Test
    void baBsGetir_gecersizDonemHataFirlatir() {
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> raporService.baBsGetir("2026-13", "BS", null, 1L));
        assertThrows(com.raspel.erp.exception.BusinessException.class,
                () -> raporService.baBsGetir(null, "BS", null, 1L));
    }

    private com.raspel.erp.entity.ticaret.Iade iade(Long id, Long faturaId) {
        com.raspel.erp.entity.ticaret.Iade i = new com.raspel.erp.entity.ticaret.Iade();
        i.setId(id);
        i.setFaturaId(faturaId);
        i.setTur("SATIS");
        i.setDurum("TAMAMLANDI");
        i.setTarih(LocalDate.of(2026, 1, id.intValue()));
        // BA/BS esigi 5000; kayit eklenmesi icin tutar esigi asmali.
        i.setTutar(new BigDecimal("6000"));
        return i;
    }

    private com.raspel.erp.entity.ticaret.IadeKalem iadeKalem(Long id, Long iadeId, String matrah, String kdvOrani) {
        com.raspel.erp.entity.ticaret.IadeKalem k = new com.raspel.erp.entity.ticaret.IadeKalem();
        k.setId(id);
        k.setIadeId(iadeId);
        k.setMiktar(BigDecimal.ONE);
        k.setBirimFiyat(new BigDecimal(matrah));
        k.setKdvOrani(new BigDecimal(kdvOrani));
        return k;
    }
}
