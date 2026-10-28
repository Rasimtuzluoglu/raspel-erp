package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.DashboardAkisDTO;
import com.raspel.erp.dto.sistem.DashboardDTO;
import com.raspel.erp.dto.sistem.DashboardGorevDTO;
import com.raspel.erp.dto.sistem.DashboardGrafikDTO;
import com.raspel.erp.dto.sistem.DashboardIcgoruDTO;
import com.raspel.erp.dto.sistem.DashboardOzetDTO;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.entity.finans.Hareket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.repository.ik.PersonelIzinRepository;
import com.raspel.erp.repository.ik.PersonelMasrafTalepRepository;
import com.raspel.erp.repository.ik.PersonelRepository;
import com.raspel.erp.repository.ticaret.SatinalmaTalepRepository;
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.repository.envanter.StokHareketRepository;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.BankaRepository;
import com.raspel.erp.repository.finans.KasaRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import com.raspel.erp.repository.sistem.OnayKararRepository;
import com.raspel.erp.repository.sistem.SirketHedefRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.entity.sistem.SirketHedef;

/**
 * Anasayfa verilerinin tek kaynağı.
 *
 * <p>Legacy {@code GET /api/dashboard} sözleşmesi korunur; yeni parçalı
 * uçlar ({@code /ozet}, {@code /gorevler}, {@code /grafikler}, {@code /akis},
 * {@code /icgoruler}) aynı yardımcı hesapları kullanır ki iki uç arasında veri
 * farkı oluşmasın.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    /** Sipariş akışında "bekleyen" sayılan durumlar (TEKLIF + saha portali BEKLIYOR). */
    private static final List<String> SIPARIS_BEKLEYEN_DURUMLAR = List.of("TEKLIF", "BEKLIYOR");
    /** Teslimat/sipariş kapanmış durumları. */
    private static final List<String> TESLIMAT_KAPALI_DURUMLAR = List.of("TESLIM_EDILDI", "IPTAL");
    /** Faturada ödeme tamamlanmış/iptal durumları (vade sorgularından hariç). */
    private static final List<String> ODEME_KAPALI_DURUMLAR = List.of("ODENDI", "IPTAL");
    /** Akış ucunda dönebilecek en fazla olay. */
    private static final int AKIS_MAKS_LIMIT = 50;

    private final CariHesapService cariHesapService;
    private final HareketService hareketService;
    private final HareketRepository hareketRepository;
    private final SiparisRepository siparisRepository;
    private final PersonelRepository personelRepository;
    private final PersonelIzinRepository personelIzinRepository;
    private final PersonelMasrafTalepRepository personelMasrafTalepRepository;
    private final SatinalmaTalepRepository satinalmaTalepRepository;
    private final StokHareketRepository stokHareketRepository;
    private final StokRepository stokRepository;
    private final FaturaRepository faturaRepository;
    private final FaturaKalemRepository faturaKalemRepository;
    private final CariHesapRepository cariHesapRepository;
    private final BankaRepository bankaRepository;
    private final KasaRepository kasaRepository;
    private final MasrafRepository masrafRepository;
    private final IadeRepository iadeRepository;
    private final SirketHedefRepository sirketHedefRepository;
    private final SirketRepository sirketRepository;
    private final TeslimatRepository teslimatRepository;
    private final OnayKararRepository onayKararRepository;

    // ==================================================================
    // Legacy uç — sözleşme DEĞİŞMEZ
    // ==================================================================

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'dashboard:' + #sirketId")
    public DashboardDTO dashboardVerileriGetir(Long sirketId) {
        log.debug("Dashboard verileri getiriliyor... sirketId: {}", sirketId);
        veriEksikTl.set(Boolean.FALSE);

        Long toplamCariSayisi = safeGet(() -> cariHesapService.toplamCariSayisiGetir(sirketId), 0L);
        BigDecimal toplamBakiye = safeGet(() -> cariHesapService.toplamBakiyeGetir(sirketId), BigDecimal.ZERO);
        List<HareketDTO> sonHareketler = safeGetList(() -> hareketService.sonHareketleriGetir(5, sirketId), Collections.emptyList());

        Long aktifCalisan = safeGet(() -> personelRepository.countByAktifTrueAndSirketId(sirketId), 0L);
        Long bugunIzinli = safeGet(() -> personelIzinRepository.countBugunIzinliAndSirketId(LocalDate.now(), sirketId), 0L);
        var ayBaslangic = LocalDate.now().withDayOfMonth(1);
        var ayBitis = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        Long buAyIseBaslayacak = safeGet(() -> personelRepository.countBySirketIdAndIseGirisTarihiBetween(sirketId, ayBaslangic, ayBitis), 0L);

        Long bugunkuSiparis = safeGet(() -> siparisRepository.countBySirketIdAndTarih(sirketId, LocalDate.now()), 0L);
        // Teslim edilmemis ve iptal edilmemis siparisler "bekleyen teslimat"tir.
        Long bekleyenTeslimat = bekleyenTeslimatSayisi(sirketId);

        long toplamStok = safeGet(() -> stokRepository.countBySirketId(sirketId), 0L);
        long toplamCikis = safeGet(() -> stokHareketRepository.countByStokSirketIdAndTur(sirketId, "CIKIS"), 0L);
        BigDecimal stokDevirHizi = toplamStok > 0
                ? BigDecimal.valueOf(toplamCikis).divide(BigDecimal.valueOf(toplamStok), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<DashboardDTO.EnCokSatanDTO> enCokSatanlar = enCokSatanlarHesapla(sirketId, 5);

        BigDecimal pozitifBakiye = safeGet(() -> cariHesapService.toplamPozitifBakiyeGetir(sirketId), BigDecimal.ZERO);
        BigDecimal negatifBakiye = safeGet(() -> cariHesapService.toplamNegatifBakiyeGetir(sirketId), BigDecimal.ZERO);

        BigDecimal bugunkuTahsilat = safeGet(() -> hareketRepository.sumTutarByTurAndHareketTarihi(Hareket.HareketTuru.TAHSILAT, LocalDate.now(), sirketId), BigDecimal.ZERO);
        BigDecimal bugunkuOdeme = safeGet(() -> hareketRepository.sumTutarByTurAndHareketTarihi(Hareket.HareketTuru.ODEME, LocalDate.now(), sirketId), BigDecimal.ZERO);
        Long bekleyenIzinSayisi = safeGet(() -> personelIzinRepository.countByDurumAndSirketId("BEKLEMEDE", sirketId), 0L);

        List<DashboardDTO.AylikGelirGiderDTO> aylikGelirGider = aylikGelirGiderHesapla(sirketId);
        List<DashboardDTO.GunlukNakitAkisiDTO> gunlukNakitAkisi = gunlukNakitAkisiHesapla(sirketId, 7);

        var bugun = LocalDate.now();
        // Dashboard yalnizca ust N bildirimi gosterir; sorgular sayfa ile sinirlandirilir
        // (aksi halde tum faturalar entity olarak yuklenip bellek/OOM riski olusturur).
        var vadeSayfa = PageRequest.of(0, 5);
        List<DashboardDTO.VadeBildirimiDTO> vadesiGecenFaturalar = safeGetList(
                () -> faturaRepository.findVadesiGecen(sirketId, Fatura.FaturaDurum.KESILDI, ODEME_KAPALI_DURUMLAR, bugun, vadeSayfa)
                        .stream().map(this::vadeDTOyaCevir).collect(Collectors.toList()),
                Collections.emptyList());
        List<DashboardDTO.VadeBildirimiDTO> vadesiYaklasanFaturalar = safeGetList(
                () -> faturaRepository.findVadesiYaklasan(sirketId, Fatura.FaturaDurum.KESILDI, ODEME_KAPALI_DURUMLAR,
                                bugun, bugun.plusDays(7), vadeSayfa)
                        .stream().map(this::vadeDTOyaCevir).collect(Collectors.toList()),
                Collections.emptyList());

        Long toplamFatura = safeGet(() -> faturaRepository.countBySirketId(sirketId), 0L);
        Long kesilenFatura = safeGet(() -> faturaRepository.countBySirketIdAndDurum(sirketId, Fatura.FaturaDurum.KESILDI), 0L);
        KasaBanka kasaBanka = kasaBankaHesapla(sirketId);
        BigDecimal toplamBankaBakiye = kasaBanka.banka();
        BigDecimal toplamKasaBakiye = kasaBanka.kasa();
        Long kritikStokSayisi = kritikStokSayisi(sirketId);

        List<DashboardDTO.KritikStokDTO> kritikStoklar = safeGetList(
                // Limit DB'de uygulanir; tum kritik stoklarin entity olarak yuklenmesi onlenir.
                () -> stokRepository.kritikStoklar(sirketId, PageRequest.of(0, 6)).stream()
                        .map(s -> DashboardDTO.KritikStokDTO.builder()
                                .id(s.getId()).stokKodu(s.getStokKodu()).ad(s.getAd())
                                .miktar(s.getMiktar()).birim(s.getBirim()).minMiktar(s.getMinMiktar()).build())
                        .collect(Collectors.toList()),
                Collections.emptyList());

        // Aylık hedef, gerçekleşen ciro ve kâr (tek kaynak: hedefVerisiHesapla).
        HedefVerisi hedef = hedefVerisiHesapla(sirketId);
        BigDecimal hedefCiro = hedef.hedefCiro();
        BigDecimal hedefKar = hedef.hedefKar();
        BigDecimal gerceklesenCiro = hedef.gerceklesenCiro();
        BigDecimal gerceklesenKar = hedef.gerceklesenKar();

        BigDecimal iadeOrani = iadeOraniHesapla(sirketId, gerceklesenCiro);
        BigDecimal ciroIlerlemeYuzdesi = yuzde(gerceklesenCiro, hedefCiro);

        List<DashboardDTO.CariOzetDTO> enCokBorcCariler = enCokBorcCarilerHesapla(sirketId);
        List<DashboardDTO.CariOzetDTO> enCokAlacakCariler = enCokAlacakCarilerHesapla(sirketId);
        List<DashboardDTO.KategoriSatisDTO> kategoriSatislari = kategoriSatislariHesapla(sirketId);
        List<DashboardDTO.AlacakYasDTO> alacakYaslandirma = alacakYaslandirmaHesapla(sirketId);

        // Stok degeri tek SQL toplaminda hesaplanir; tum stoklarin yuklenmesi onlenir (bellek).
        BigDecimal toplamStokDegeri = safeGet(
                () -> stokRepository.toplamStokDegeriBySirketId(sirketId),
                BigDecimal.ZERO);

        String ozet = ozetOlustur(gerceklesenCiro, kritikStokSayisi, vadesiGecenFaturalar, enCokBorcCariler);

        return DashboardDTO.builder()
                .veriEksik(veriEksikTl.get())
                .toplamCariSayisi(toplamCariSayisi)
                .toplamBakiye(toplamBakiye)
                .sonHareketler(sonHareketler)
                .aktifCalisan(aktifCalisan)
                .bugunIzinli(bugunIzinli)
                .buAyIseBaslayacak(buAyIseBaslayacak)
                .bugunkuSiparis(bugunkuSiparis)
                .bekleyenTeslimat(bekleyenTeslimat)
                .iadeOrani(iadeOrani)
                .toplamStok(toplamStok)
                .kritikStokSayisi(kritikStokSayisi)
                .kritikStoklar(kritikStoklar)
                .toplamFatura(toplamFatura)
                .kesilenFatura(kesilenFatura)
                .toplamBankaBakiye(toplamBankaBakiye)
                .toplamKasaBakiye(toplamKasaBakiye)
                .hedefCiro(hedefCiro)
                .gerceklesenCiro(gerceklesenCiro)
                .ciroIlerlemeYuzdesi(ciroIlerlemeYuzdesi)
                .hedefKar(hedefKar)
                .gerceklesenKar(gerceklesenKar)
                .stokDevirHizi(stokDevirHizi)
                .enCokSatanlar(enCokSatanlar)
                .pozitifBakiye(pozitifBakiye)
                .negatifBakiye(negatifBakiye)
                .bugunkuTahsilat(bugunkuTahsilat)
                .bugunkuOdeme(bugunkuOdeme)
                .bekleyenIzinSayisi(bekleyenIzinSayisi)
                .aylikGelirGider(aylikGelirGider)
                .gunlukNakitAkisi(gunlukNakitAkisi)
                .vadesiGecenFaturalar(vadesiGecenFaturalar)
                .vadesiYaklasanFaturalar(vadesiYaklasanFaturalar)
                .enCokBorcCariler(enCokBorcCariler)
                .enCokAlacakCariler(enCokAlacakCariler)
                .kategoriSatislari(kategoriSatislari)
                .alacakYaslandirma(alacakYaslandirma)
                .toplamStokDegeri(toplamStokDegeri)
                .ozet(ozet)
                .build();
    }

    // ==================================================================
    // Hızlı özet — ilk boya
    // ==================================================================

    /**
     * Anasayfa hızlı özeti.
     *
     * <p>Yalnızca COUNT/SUM ve tek sorguluk seriler kullanılır; hiçbir liste
     * entity olarak yüklenmez. Hero kullanıcı adı taşıdığı için cache anahtarı
     * kullanıcıyı da içerir (roller arası isim sızmasını önler).
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'ozet:' + #sirketId + ':' + #rol + ':' + #kullaniciId")
    public DashboardOzetDTO ozetGetir(Long sirketId, Long kullaniciId, String kullaniciAd, String rol) {
        log.debug("Dashboard ozeti hazirlaniyor... sirketId: {}, rol: {}", sirketId, rol);

        HedefVerisi hedef = hedefVerisiHesapla(sirketId);
        KasaBanka kasaBanka = kasaBankaHesapla(sirketId);
        BigDecimal likidite = nz(kasaBanka.kasa()).add(nz(kasaBanka.banka()));

        List<BigDecimal> sparkline = sparklineHesapla(sirketId);
        BigDecimal netYediGun = sparkline.stream().map(this::nz).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal trendYuzde = likidite.compareTo(BigDecimal.ZERO) != 0
                ? netYediGun.multiply(BigDecimal.valueOf(100)).divide(likidite.abs(), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal alacak = nz(safeGet(() -> cariHesapService.toplamPozitifBakiyeGetir(sirketId), BigDecimal.ZERO));
        BigDecimal borc = nz(safeGet(() -> cariHesapService.toplamNegatifBakiyeGetir(sirketId), BigDecimal.ZERO)).abs();
        BigDecimal netCari = alacak.subtract(borc);

        long kritikStok = safeGet(() -> stokRepository.countKritikStokBySirketId(sirketId), 0L);
        BigDecimal marj = yuzde(hedef.gerceklesenKar(), hedef.gerceklesenCiro());

        String sirketAd = safeGet(() -> sirketRepository.findById(sirketId).map(s -> s.getAd()).orElse(null), null);

        LocalDate bugun = LocalDate.now();
        BigDecimal bugunkuTahsilat = safeGet(() -> hareketRepository.sumTutarByTurAndHareketTarihi(
                Hareket.HareketTuru.TAHSILAT, bugun, sirketId), BigDecimal.ZERO);
        BigDecimal bugunkuOdeme = safeGet(() -> hareketRepository.sumTutarByTurAndHareketTarihi(
                Hareket.HareketTuru.ODEME, bugun, sirketId), BigDecimal.ZERO);
        Long bugunkuSiparis = safeGet(() -> siparisRepository.countBySirketIdAndTarih(sirketId, bugun), 0L);
        Long bugunkuTeslimat = safeGet(() -> teslimatRepository
                .countBySirketIdAndBeklenenTeslimTarihiAndDurumNotIn(sirketId, bugun, TESLIMAT_KAPALI_DURUMLAR), 0L);

        long onayBekleyen = onayBekleyenSayisi(sirketId);
        long vadesiGecen = vadesiGecenSayisi(sirketId);
        long vadesiYaklasan = vadesiYaklasanSayisi(sirketId);
        long bekleyenTeslimat = bekleyenTeslimatSayisi(sirketId);

        return DashboardOzetDTO.builder()
                .hero(DashboardOzetDTO.HeroDTO.builder()
                        .kullaniciAd(kullaniciAd)
                        .sirketAd(sirketAd)
                        .canli(Boolean.TRUE)
                        .build())
                .bugun(DashboardOzetDTO.BugunDTO.builder()
                        .tahsilat(bugunkuTahsilat)
                        .odeme(bugunkuOdeme)
                        .siparis(bugunkuSiparis)
                        .teslimat(bugunkuTeslimat)
                        .build())
                .kpi(DashboardOzetDTO.KpiDTO.builder()
                        .likidite(DashboardOzetDTO.LikiditeDTO.builder()
                                .deger(likidite)
                                .kasa(nz(kasaBanka.kasa()))
                                .banka(nz(kasaBanka.banka()))
                                .trendYuzde(trendYuzde)
                                .sparkline(sparkline)
                                .build())
                        .netCari(DashboardOzetDTO.NetCariDTO.builder()
                                .deger(netCari)
                                .alacak(alacak)
                                .borc(borc)
                                .build())
                        .ciro(DashboardOzetDTO.CiroDTO.builder()
                                .deger(hedef.gerceklesenCiro())
                                .hedef(hedef.hedefCiro())
                                .yuzde(yuzde(hedef.gerceklesenCiro(), hedef.hedefCiro()))
                                .build())
                        .kar(DashboardOzetDTO.KarDTO.builder()
                                .deger(hedef.gerceklesenKar())
                                .hedef(hedef.hedefKar())
                                .marj(marj)
                                .build())
                        .build())
                .stok(DashboardOzetDTO.StokDTO.builder()
                        .cesit(safeGet(() -> stokRepository.countBySirketId(sirketId), 0L))
                        .deger(safeGet(() -> stokRepository.toplamStokDegeriBySirketId(sirketId), BigDecimal.ZERO))
                        .kritikAdet(kritikStok)
                        .build())
                .gorevSayilari(DashboardOzetDTO.GorevSayilariDTO.builder()
                        .onayBekleyen(onayBekleyen)
                        .vadesiGecen(vadesiGecen)
                        .kritikStok(kritikStok)
                        .bekleyenTeslimat(bekleyenTeslimat)
                        .vadesiYaklasan(vadesiYaklasan)
                        .build())
                .build();
    }

    // ==================================================================
    // Görev merkezi
    // ==================================================================

    /**
     * Rol filtreli, öncelik sıralı görev kartları.
     *
     * <p>ADMIN/MUHASEBE tüm görevleri görür; USER yalnızca vade, kritik stok ve
     * teslimat görevlerini görür (onay yetkisi olmadığı için onay kartları
     * üretilmez). Adedi sıfır olan kartlar döndürülmez.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'gorevler:' + #sirketId + ':' + #rol")
    public List<DashboardGorevDTO> gorevlerGetir(Long sirketId, String rol) {
        boolean onayGorebilir = "ADMIN".equals(rol) || "MUHASEBE".equals(rol);
        LocalDate bugun = LocalDate.now();
        List<DashboardGorevDTO> gorevler = new ArrayList<>();

        if (onayGorebilir) {
            long izin = safeGet(() -> personelIzinRepository.countByDurumAndSirketId("BEKLEMEDE", sirketId), 0L);
            gorevler.add(gorev("ONAY_IZIN", "İzin Onayı Bekliyor", " adet izin talebi onay bekliyor",
                    izin, null, 90, "/onaylar?tur=IZIN", "pi pi-calendar", "#3b82f6"));

            long masraf = safeGet(() -> personelMasrafTalepRepository.countBySirketIdAndDurum(sirketId, "BEKLEMEDE"), 0L);
            BigDecimal masrafTutar = safeGet(() -> personelMasrafTalepRepository.sumTutarBySirketIdAndDurum(sirketId, "BEKLEMEDE"), BigDecimal.ZERO);
            gorevler.add(gorev("ONAY_MASRAF", "Masraf/Avans Onayı Bekliyor", " adet masraf veya avans talebi onay bekliyor",
                    masraf, masrafTutar, 85, "/onaylar?tur=MASRAF", "pi pi-wallet", "#f59e0b"));

            long satinalma = safeGet(() -> satinalmaTalepRepository.countBySirketIdAndDurum(sirketId, "TASLAK"), 0L);
            gorevler.add(gorev("ONAY_SATINALMA", "Satınalma Talebi Onayı Bekliyor", " adet satınalma talebi onay bekliyor",
                    satinalma, null, 82, "/onaylar?tur=SATINALMA", "pi pi-shopping-bag", "#8b5cf6"));

            long siparis = safeGet(() -> siparisRepository.countBySirketIdAndDurumIn(sirketId, SIPARIS_BEKLEYEN_DURUMLAR), 0L);
            BigDecimal siparisTutar = safeGet(() -> siparisRepository.sumTutarBySirketIdAndDurumIn(sirketId, SIPARIS_BEKLEYEN_DURUMLAR), BigDecimal.ZERO);
            gorevler.add(gorev("ONAY_SIPARIS", "Sipariş Onayı Bekliyor", " adet sipariş onay bekliyor",
                    siparis, siparisTutar, 80, "/onaylar?tur=SIPARIS", "pi pi-file-edit", "#6366f1"));
        }

        long vadesiGecen = vadesiGecenSayisi(sirketId);
        BigDecimal vadesiGecenTutar = safeGet(() -> faturaRepository.toplamVadesiGecenKalan(
                sirketId, Fatura.FaturaDurum.KESILDI, ODEME_KAPALI_DURUMLAR, bugun), BigDecimal.ZERO);
        gorevler.add(gorev("VADESI_GECEN", "Vadesi Geçen Fatura", " faturanın vadesi geçmiş durumda",
                vadesiGecen, vadesiGecenTutar, 95, "/tahsilat", "pi pi-exclamation-triangle", "#ef4444"));

        long vadesiYaklasan = vadesiYaklasanSayisi(sirketId);
        BigDecimal vadesiYaklasanTutar = safeGet(() -> faturaRepository.toplamKalanVadeAraliginda(
                        sirketId, Fatura.FaturaDurum.KESILDI, ODEME_KAPALI_DURUMLAR, bugun, bugun.plusDays(7)),
                BigDecimal.ZERO);
        gorevler.add(gorev("VADESI_YAKLASAN", "Vadesi Yaklaşan Fatura", " faturanın vadesi 7 gün içinde doluyor",
                vadesiYaklasan, vadesiYaklasanTutar, 50, "/tahsilat", "pi pi-clock", "#eab308"));

        long kritikStok = kritikStokSayisi(sirketId);
        gorevler.add(gorev("KRITIK_STOK", "Kritik Stok Seviyesi", " ürün minimum stok seviyesinde veya altında",
                kritikStok, null, 70, "/stoklar?kritik=1", "pi pi-box", "#f97316"));

        long bekleyenTeslimat = bekleyenTeslimatSayisi(sirketId);
        gorevler.add(gorev("BEKLEYEN_TESLIMAT", "Bekleyen Teslimat", " sipariş teslim edilmeyi bekliyor",
                bekleyenTeslimat, null, 60, "/teslimatlar", "pi pi-truck", "#14b8a6"));

        return gorevler.stream()
                .filter(g -> g.getAdet() != null && g.getAdet() > 0)
                .sorted(Comparator.comparing(DashboardGorevDTO::getOncelik).reversed()
                        .thenComparing(DashboardGorevDTO::getTur))
                .toList();
    }

    private DashboardGorevDTO gorev(String tur, String baslik, String aciklamaEki, long adet, BigDecimal tutar,
                                    int oncelik, String route, String ikon, String renk) {
        return DashboardGorevDTO.builder()
                .tur(tur)
                .baslik(baslik)
                .aciklama(adet + aciklamaEki)
                .adet(adet)
                .tutar(tutar)
                .oncelik(oncelik)
                .route(route)
                .ikon(ikon)
                .renk(renk)
                .build();
    }

    // ==================================================================
    // Grafikler
    // ==================================================================

    /**
     * Ana + genişletilmiş grafik verileri. Legacy uçtaki hesapların aynısını
     * kullanır (tek kaynak), ancak 30 günlük nakit akışı döner.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'grafikler:' + #sirketId")
    public DashboardGrafikDTO grafiklerGetir(Long sirketId) {
        HedefVerisi hedef = hedefVerisiHesapla(sirketId);
        KasaBanka kasaBanka = kasaBankaHesapla(sirketId);

        return DashboardGrafikDTO.builder()
                .aylikGelirGider(aylikGelirGiderHesapla(sirketId))
                .gunlukNakitAkisi(gunlukNakitAkisiHesapla(sirketId, 30))
                .enCokSatanlar(enCokSatanlarHesapla(sirketId, 5))
                .kategoriSatislari(kategoriSatislariHesapla(sirketId))
                .alacakYaslandirma(alacakYaslandirmaHesapla(sirketId))
                .enCokBorcCariler(enCokBorcCarilerHesapla(sirketId))
                .enCokAlacakCariler(enCokAlacakCarilerHesapla(sirketId))
                .kasaBanka(DashboardGrafikDTO.KasaBankaDTO.builder()
                        .kasa(nz(kasaBanka.kasa()))
                        .banka(nz(kasaBanka.banka()))
                        .build())
                .hedef(DashboardGrafikDTO.HedefDTO.builder()
                        .ciro(hedef.hedefCiro())
                        .gerceklesen(hedef.gerceklesenCiro())
                        .kar(hedef.gerceklesenKar())
                        .hedefKar(hedef.hedefKar())
                        .build())
                .build();
    }

    // ==================================================================
    // Canlı akış
    // ==================================================================

    /**
     * Birleşik olay akışı: onay kararı + hareket + fatura + sipariş + stok.
     *
     * <p>Her kaynaktan {@code limit} kadar okunur, birleştirilir, tarihe göre
     * azalan sıralanır ve toplam {@code limit} (en fazla 50) döner. Tenant
     * zorunludur.
     */
    @Transactional(readOnly = true)
    public List<DashboardAkisDTO> akisGetir(Long sirketId, int limit) {
        if (sirketId == null) {
            return Collections.emptyList();
        }
        int istenen = Math.max(1, Math.min(limit, AKIS_MAKS_LIMIT));
        var sayfa = PageRequest.of(0, istenen);

        List<DashboardAkisDTO> tum = new ArrayList<>();
        tum.addAll(onayAkislari(sirketId, sayfa));
        tum.addAll(hareketAkislari(sirketId, sayfa));
        tum.addAll(faturaAkislari(sirketId, sayfa));
        tum.addAll(siparisAkislari(sirketId, sayfa));
        tum.addAll(stokAkislari(sirketId, sayfa));

        return tum.stream()
                .sorted(Comparator.comparing(DashboardAkisDTO::getTarih,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .limit(istenen)
                .toList();
    }

    private List<DashboardAkisDTO> onayAkislari(Long sirketId, PageRequest sayfa) {
        return safeGetList(() -> onayKararRepository.findBySirketIdOrderByOlusturmaTarihiDesc(sirketId, sayfa).stream()
                .map(k -> {
                    boolean onay = "ONAY".equals(k.getKarar());
                    String aciklama = (onay ? "Onaylandı" : "Reddedildi")
                            + (k.getGerekce() != null && !k.getGerekce().isBlank() ? " — " + k.getGerekce() : "")
                            + (k.getKararVerenAd() != null && !k.getKararVerenAd().isBlank() ? " (" + k.getKararVerenAd() + ")" : "");
                    return DashboardAkisDTO.builder()
                            .tur("ONAY")
                            .baslik(onayTurEtiketi(k.getTur()) + " onay kararı")
                            .aciklama(aciklama)
                            .tutar(null)
                            .tarih(k.getOlusturmaTarihi())
                            .route("/onaylar")
                            .ikon(onay ? "pi pi-check-circle" : "pi pi-times-circle")
                            .renk(onay ? "#22c55e" : "#ef4444")
                            .build();
                })
                .collect(Collectors.toList()), Collections.emptyList());
    }

    private List<DashboardAkisDTO> hareketAkislari(Long sirketId, PageRequest sayfa) {
        return safeGetList(() -> hareketRepository
                .findBySirketIdOrderByHareketTarihiDescOlusturmaTarihiDesc(sirketId, sayfa).stream()
                .filter(h -> h.getTur() == Hareket.HareketTuru.TAHSILAT || h.getTur() == Hareket.HareketTuru.ODEME)
                .map(h -> {
                    boolean tahsilat = h.getTur() == Hareket.HareketTuru.TAHSILAT;
                    String cariAd = h.getCariHesap() != null ? h.getCariHesap().getAd() : null;
                    return DashboardAkisDTO.builder()
                            .tur(tahsilat ? "TAHSILAT" : "ODEME")
                            .baslik((tahsilat ? "Tahsilat" : "Ödeme") + (cariAd != null ? " - " + cariAd : ""))
                            .aciklama(h.getAciklama())
                            .tutar(h.getTutar())
                            .tarih(h.getOlusturmaTarihi() != null
                                    ? h.getOlusturmaTarihi()
                                    : h.getHareketTarihi() != null ? h.getHareketTarihi().atStartOfDay() : null)
                            .route("/hareketler")
                            .ikon(tahsilat ? "pi pi-arrow-down-left" : "pi pi-arrow-up-right")
                            .renk(tahsilat ? "#22c55e" : "#ef4444")
                            .build();
                })
                .collect(Collectors.toList()), Collections.emptyList());
    }

    private List<DashboardAkisDTO> faturaAkislari(Long sirketId, PageRequest sayfa) {
        return safeGetList(() -> faturaRepository.findBySirketIdOrderByTarihDesc(sirketId, sayfa).getContent().stream()
                .map(f -> DashboardAkisDTO.builder()
                        .tur("FATURA")
                        .baslik(f.getFaturaNumarasi() + " numaralı fatura")
                        .aciklama((f.getCariHesap() != null ? f.getCariHesap().getAd() + " · " : "")
                                + (f.getTur() != null ? f.getTur().name() : ""))
                        .tutar(f.getGenelToplam())
                        .tarih(f.getOlusturmaTarihi() != null
                                ? f.getOlusturmaTarihi()
                                : f.getTarih() != null ? f.getTarih().atStartOfDay() : null)
                        .route("/faturalar")
                        .ikon("pi pi-file")
                        .renk("#3b82f6")
                        .build())
                .collect(Collectors.toList()), Collections.emptyList());
    }

    private List<DashboardAkisDTO> siparisAkislari(Long sirketId, PageRequest sayfa) {
        return safeGetList(() -> siparisRepository.findBySirketIdOrderByTarihDesc(sirketId, sayfa).getContent().stream()
                .map(s -> DashboardAkisDTO.builder()
                        .tur("SIPARIS")
                        .baslik(s.getSiparisNo() + " numaralı sipariş")
                        .aciklama(s.getDurum())
                        .tutar(s.getGenelToplam())
                        .tarih(s.getOlusturmaTarihi() != null
                                ? s.getOlusturmaTarihi()
                                : s.getTarih() != null ? s.getTarih().atStartOfDay() : null)
                        .route("/siparisler")
                        .ikon("pi pi-shopping-cart")
                        .renk("#8b5cf6")
                        .build())
                .collect(Collectors.toList()), Collections.emptyList());
    }

    private List<DashboardAkisDTO> stokAkislari(Long sirketId, PageRequest sayfa) {
        return safeGetList(() -> stokHareketRepository.findByStokSirketId(sirketId, sayfa).getContent().stream()
                .map(h -> {
                    String stokAd = h.getStok() != null ? h.getStok().getAd() : "Stok";
                    boolean cikis = "CIKIS".equals(h.getTur());
                    return DashboardAkisDTO.builder()
                            .tur("STOK")
                            .baslik(stokAd + " · " + (cikis ? "Çıkış" : "Giriş"))
                            .aciklama(h.getAciklama())
                            .tutar(null)
                            .tarih(h.getOlusturmaTarihi() != null
                                    ? h.getOlusturmaTarihi()
                                    : h.getHareketTarihi() != null ? h.getHareketTarihi().atStartOfDay() : null)
                            .route("/stoklar")
                            .ikon("pi pi-box")
                            .renk(cikis ? "#f59e0b" : "#22c55e")
                            .build();
                })
                .collect(Collectors.toList()), Collections.emptyList());
    }

    // ==================================================================
    // İçgörüler
    // ==================================================================

    /**
     * Kural tabanlı içgörüler (en fazla 3, önce KRITIK sonra WARN).
     *
     * <p>Kurallar: ciro hedefi, vadesi geçen fatura, kritik stok, 7 günlük
     * likidite trendi.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'icgoruler:' + #sirketId")
    public List<DashboardIcgoruDTO> icgorulerGetir(Long sirketId) {
        List<DashboardIcgoruDTO> icgoruler = new ArrayList<>();

        HedefVerisi hedef = hedefVerisiHesapla(sirketId);
        BigDecimal ciroYuzde = yuzde(hedef.gerceklesenCiro(), hedef.hedefCiro());
        if (hedef.hedefCiro() != null && hedef.hedefCiro().compareTo(BigDecimal.ZERO) > 0
                && ciroYuzde.compareTo(BigDecimal.valueOf(50)) < 0) {
            boolean kritik = ciroYuzde.compareTo(BigDecimal.valueOf(25)) < 0;
            icgoruler.add(DashboardIcgoruDTO.builder()
                    .seviye(kritik ? "KRITIK" : "WARN")
                    .baslik("Ciro hedefin gerisinde")
                    .mesaj("Bu ay ciro hedefinin %" + ciroYuzde.toPlainString() + " kadarı gerçekleşti ("
                            + hedef.gerceklesenCiro().toPlainString() + " / " + hedef.hedefCiro().toPlainString() + " TL).")
                    .route("/raporlar")
                    .ikon("pi pi-chart-line")
                    .build());
        }

        long vadesiGecen = vadesiGecenSayisi(sirketId);
        if (vadesiGecen > 0) {
            BigDecimal tutar = safeGet(() -> faturaRepository.toplamVadesiGecenKalan(
                    sirketId, Fatura.FaturaDurum.KESILDI, ODEME_KAPALI_DURUMLAR, LocalDate.now()), BigDecimal.ZERO);
            boolean kritik = vadesiGecen >= 5;
            icgoruler.add(DashboardIcgoruDTO.builder()
                    .seviye(kritik ? "KRITIK" : "WARN")
                    .baslik("Vadesi geçen faturalar var")
                    .mesaj(vadesiGecen + " faturanın vadesi geçmiş; toplam " + nz(tutar).toPlainString() + " TL tahsil edilmeli.")
                    .route("/tahsilat")
                    .ikon("pi pi-exclamation-triangle")
                    .build());
        }

        long kritikStok = kritikStokSayisi(sirketId);
        if (kritikStok > 0) {
            boolean kritik = kritikStok >= 10;
            icgoruler.add(DashboardIcgoruDTO.builder()
                    .seviye(kritik ? "KRITIK" : "WARN")
                    .baslik("Kritik stok seviyesi")
                    .mesaj(kritikStok + " ürün minimum stok seviyesinde veya altında; sipariş planlaması önerilir.")
                    .route("/stoklar?kritik=1")
                    .ikon("pi pi-box")
                    .build());
        }

        BigDecimal netYediGun = sparklineHesapla(sirketId).stream().map(this::nz).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (netYediGun.compareTo(BigDecimal.ZERO) < 0) {
            icgoruler.add(DashboardIcgoruDTO.builder()
                    .seviye("WARN")
                    .baslik("Nakit akışı negatif")
                    .mesaj("Son 7 günde net nakit akışı " + netYediGun.toPlainString()
                            + " TL; ödeme planını gözden geçirin.")
                    .route("/kasa")
                    .ikon("pi pi-wallet")
                    .build());
        }

        return icgoruler.stream()
                .sorted(Comparator.comparingInt((DashboardIcgoruDTO i) -> seviyeSirasi(i.getSeviye())))
                .limit(3)
                .toList();
    }

    private int seviyeSirasi(String seviye) {
        if ("KRITIK".equals(seviye)) return 0;
        if ("WARN".equals(seviye)) return 1;
        return 2;
    }

    // ==================================================================
    // Ortak hesaplar (legacy + yeni uçlar aynı kaynak)
    // ==================================================================

    private List<DashboardDTO.AylikGelirGiderDTO> aylikGelirGiderHesapla(Long sirketId) {
        var onIkiAyOnce = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        return safeGetList(() -> hareketRepository.aylikGelirGider(onIkiAyOnce, sirketId), Collections.emptyList())
                .stream().map(row -> DashboardDTO.AylikGelirGiderDTO.builder()
                        .ay((String) row[0])
                        .gelir((BigDecimal) row[1])
                        .gider((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardDTO.GunlukNakitAkisiDTO> gunlukNakitAkisiHesapla(Long sirketId, int gunSayisi) {
        var baslangic = LocalDate.now().minusDays(Math.max(1, gunSayisi) - 1L);
        return safeGetList(() -> hareketRepository.gunlukNakitAkisi(baslangic, sirketId), Collections.emptyList())
                .stream().map(row -> DashboardDTO.GunlukNakitAkisiDTO.builder()
                        .gun((String) row[0])
                        .gelir((BigDecimal) row[1])
                        .gider((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Son 7 günün günlük net nakit akışı (eskiden yeniye). Kaydı olmayan günler
     * sıfır ile doldurulur; sparkline her zaman 7 nokta döner.
     */
    private List<BigDecimal> sparklineHesapla(Long sirketId) {
        Map<String, BigDecimal> netler = new HashMap<>();
        for (var gun : gunlukNakitAkisiHesapla(sirketId, 7)) {
            netler.put(gun.getGun(), nz(gun.getGelir()).subtract(nz(gun.getGider())));
        }
        List<BigDecimal> sparkline = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            sparkline.add(netler.getOrDefault(LocalDate.now().minusDays(i).toString(), BigDecimal.ZERO));
        }
        return sparkline;
    }

    private List<DashboardDTO.EnCokSatanDTO> enCokSatanlarHesapla(Long sirketId, int limit) {
        return safeGetList(() -> stokHareketRepository.enCokSatanlarBySirket(sirketId), Collections.emptyList())
                .stream().limit(limit)
                .map(m -> DashboardDTO.EnCokSatanDTO.builder()
                        .stokAd((String) m.get("stokAd"))
                        .stokKodu((String) m.get("stokKodu"))
                        .satisMiktari((BigDecimal) m.get("satisMiktari"))
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardDTO.CariOzetDTO> enCokBorcCarilerHesapla(Long sirketId) {
        return safeGetList(
                () -> cariHesapRepository.findTop5BySirketIdAndBakiyeLessThanOrderByBakiyeAsc(sirketId, BigDecimal.ZERO).stream()
                        .map(c -> DashboardDTO.CariOzetDTO.builder().cariAd(c.getAd()).tutar(c.getBakiye().abs()).build())
                        .collect(Collectors.toList()),
                Collections.emptyList());
    }

    private List<DashboardDTO.CariOzetDTO> enCokAlacakCarilerHesapla(Long sirketId) {
        return safeGetList(
                () -> cariHesapRepository.findTop5BySirketIdAndBakiyeGreaterThanOrderByBakiyeDesc(sirketId, BigDecimal.ZERO).stream()
                        .map(c -> DashboardDTO.CariOzetDTO.builder().cariAd(c.getAd()).tutar(c.getBakiye()).build())
                        .collect(Collectors.toList()),
                Collections.emptyList());
    }

    private List<DashboardDTO.KategoriSatisDTO> kategoriSatislariHesapla(Long sirketId) {
        return safeGetList(
                () -> faturaKalemRepository.kategoriSatislari(sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI)
                        .stream()
                        .map(row -> DashboardDTO.KategoriSatisDTO.builder()
                                .kategori((String) row[0])
                                .tutar((BigDecimal) row[1])
                                .build())
                        .collect(Collectors.toList()),
                Collections.emptyList());
    }

    private List<DashboardDTO.AlacakYasDTO> alacakYaslandirmaHesapla(Long sirketId) {
        LocalDate bugun = LocalDate.now();
        BigDecimal vadesiGecen = safeGet(
                () -> faturaRepository.toplamVadesiGecenKalan(sirketId, Fatura.FaturaDurum.KESILDI, ODEME_KAPALI_DURUMLAR, bugun),
                BigDecimal.ZERO);
        return List.of(
                DashboardDTO.AlacakYasDTO.builder().aralik("Vadesi Geçti").tutar(vadesiGecen).build(),
                DashboardDTO.AlacakYasDTO.builder().aralik("0-30 Gün")
                        .tutar(kalanTopla(sirketId, bugun, bugun.plusDays(30))).build(),
                DashboardDTO.AlacakYasDTO.builder().aralik("31-60 Gün")
                        .tutar(kalanTopla(sirketId, bugun.plusDays(31), bugun.plusDays(60))).build(),
                DashboardDTO.AlacakYasDTO.builder().aralik("60+ Gün")
                        .tutar(kalanTopla(sirketId, bugun.plusDays(61), bugun.plusDays(3650))).build());
    }

    private BigDecimal kalanTopla(Long sirketId, LocalDate bas, LocalDate bit) {
        return safeGet(
                () -> faturaRepository.toplamKalanVadeAraliginda(sirketId, Fatura.FaturaDurum.KESILDI, ODEME_KAPALI_DURUMLAR, bas, bit),
                BigDecimal.ZERO);
    }

    private KasaBanka kasaBankaHesapla(Long sirketId) {
        BigDecimal banka = safeGet(() -> bankaRepository.sumBakiyeBySirketId(sirketId), BigDecimal.ZERO);
        BigDecimal kasa = safeGet(() -> kasaRepository.sumBakiyeBySirketId(sirketId), BigDecimal.ZERO);
        return new KasaBanka(kasa, banka);
    }

    /**
     * Aylık hedef + gerçekleşen ciro/kâr. Legacy uç ve KPI'lar aynı hesabı
     * kullanır; masraf ve alış maliyeti tek fatura listesinden türetilir.
     */
    private HedefVerisi hedefVerisiHesapla(Long sirketId) {
        LocalDate ayBas = LocalDate.now().withDayOfMonth(1);
        LocalDate aySon = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        Optional<SirketHedef> hedefOpt = safeGet(
                () -> sirketHedefRepository.findBySirketIdAndYilAndAy(sirketId, LocalDate.now().getYear(), LocalDate.now().getMonthValue()),
                Optional.empty());
        BigDecimal hedefCiro = hedefOpt.map(SirketHedef::getHedefCiro).orElse(BigDecimal.ZERO);
        BigDecimal hedefKar = hedefOpt.map(SirketHedef::getHedefKar).orElse(BigDecimal.ZERO);

        // Ayın faturaları tek sorguyla çekilir; ciro ve alış maliyeti aynı listeden hesaplanır.
        final List<Fatura> ayFaturalari = safeGetList(
                () -> faturaRepository.findBySirketIdAndTarihBetween(sirketId, ayBas, aySon),
                Collections.emptyList());
        BigDecimal gerceklesenCiro = ayFaturalari.stream()
                .filter(f -> f.getTur() == Fatura.FaturaTur.SATIS && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .map(f -> f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal toplamAlisMaliyeti = ayFaturalari.stream()
                .filter(f -> f.getTur() == Fatura.FaturaTur.ALIS && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .map(f -> f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal toplamMasraflar = safeGet(() -> masrafRepository.findBySirketIdAndTarihBetween(sirketId, ayBas, aySon).stream()
                .map(Masraf::getTutar).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add), BigDecimal.ZERO);

        BigDecimal gerceklesenKar = gerceklesenCiro.subtract(toplamAlisMaliyeti).subtract(toplamMasraflar);
        return new HedefVerisi(hedefCiro, gerceklesenCiro, hedefKar, gerceklesenKar);
    }

    /** İade oranı: dönem satış iadesi tutarının satış cirosuna oranı (yüzde). */
    private BigDecimal iadeOraniHesapla(Long sirketId, BigDecimal gerceklesenCiro) {
        LocalDate ayBas = LocalDate.now().withDayOfMonth(1);
        LocalDate aySon = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        return safeGet(() -> {
            BigDecimal iadeTutar = iadeRepository
                    .findBySirketIdAndTurAndDurumAndTarihBetween(sirketId, "SATIS", "TAMAMLANDI", ayBas, aySon)
                    .stream()
                    .map(i -> i.getTutar() != null ? i.getTutar() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return gerceklesenCiro.compareTo(BigDecimal.ZERO) > 0
                    ? iadeTutar.multiply(BigDecimal.valueOf(100)).divide(gerceklesenCiro, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }, BigDecimal.ZERO);
    }

    // --- Sayaçlar (tek sorgu; liste yüklenmez) ---

    private long onayBekleyenSayisi(Long sirketId) {
        long izin = safeGet(() -> personelIzinRepository.countByDurumAndSirketId("BEKLEMEDE", sirketId), 0L);
        long masraf = safeGet(() -> personelMasrafTalepRepository.countBySirketIdAndDurum(sirketId, "BEKLEMEDE"), 0L);
        long satinalma = safeGet(() -> satinalmaTalepRepository.countBySirketIdAndDurum(sirketId, "TASLAK"), 0L);
        long siparis = safeGet(() -> siparisRepository.countBySirketIdAndDurumIn(sirketId, SIPARIS_BEKLEYEN_DURUMLAR), 0L);
        return izin + masraf + satinalma + siparis;
    }

    private long vadesiGecenSayisi(Long sirketId) {
        return safeGet(() -> faturaRepository.countVadesiGecen(
                sirketId, Fatura.FaturaDurum.KESILDI, ODEME_KAPALI_DURUMLAR, LocalDate.now()), 0L);
    }

    private long vadesiYaklasanSayisi(Long sirketId) {
        LocalDate bugun = LocalDate.now();
        return safeGet(() -> faturaRepository.countVadesiYaklasan(
                sirketId, Fatura.FaturaDurum.KESILDI, ODEME_KAPALI_DURUMLAR, bugun, bugun.plusDays(7)), 0L);
    }

    private long kritikStokSayisi(Long sirketId) {
        return safeGet(() -> stokRepository.countKritikStokBySirketId(sirketId), 0L);
    }

    /** Legacy uçtaki "bekleyen teslimat" tanımı: teslim/iptal dışı siparişler. */
    private long bekleyenTeslimatSayisi(Long sirketId) {
        return safeGet(() -> siparisRepository.countBySirketIdAndDurumNotIn(sirketId, TESLIMAT_KAPALI_DURUMLAR), 0L);
    }

    // ==================================================================
    // Yardımcılar
    // ==================================================================

    private BigDecimal yuzde(BigDecimal pay, BigDecimal payda) {
        return payda != null && payda.compareTo(BigDecimal.ZERO) > 0
                ? nz(pay).multiply(BigDecimal.valueOf(100)).divide(payda, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private BigDecimal nz(BigDecimal deger) {
        return deger != null ? deger : BigDecimal.ZERO;
    }

    private String onayTurEtiketi(String tur) {
        if (tur == null) return "Onay";
        return switch (tur) {
            case "IZIN" -> "İzin";
            case "MASRAF" -> "Masraf";
            case "SATINALMA" -> "Satınalma";
            case "SIPARIS" -> "Sipariş";
            default -> tur;
        };
    }

    private String ozetOlustur(BigDecimal ciro, Long kritikStok, List<DashboardDTO.VadeBildirimiDTO> vadesiGecen,
                               List<DashboardDTO.CariOzetDTO> enCokBorc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bu ay kesilen satış cirosu ").append(ciro != null ? ciro.toPlainString() : "0").append(" TL.");
        if (kritikStok != null && kritikStok > 0) {
            sb.append(" ").append(kritikStok).append(" ürün kritik stok seviyesinde; sipariş planlaması önerilir.");
        }
        if (vadesiGecen != null && !vadesiGecen.isEmpty()) {
            sb.append(" ").append(vadesiGecen.size()).append(" faturanın vadesi geçmiş durumda.");
        }
        if (enCokBorc != null && !enCokBorc.isEmpty()) {
            sb.append(" En yüksek borçlu cari: ").append(enCokBorc.get(0).getCariAd()).append(".");
        }
        return sb.toString();
    }

    private DashboardDTO.VadeBildirimiDTO vadeDTOyaCevir(Fatura f) {
        return DashboardDTO.VadeBildirimiDTO.builder()
                .faturaId(f.getId())
                .faturaNumarasi(f.getFaturaNumarasi())
                .cariHesapAd(f.getCariHesap() != null ? f.getCariHesap().getAd() : null)
                .cariTelefon(f.getCariHesap() != null ? f.getCariHesap().getTelefon() : null)
                .vadeTarihi(f.getVadeTarihi())
                .kalanTutar(f.getKalanTutar())
                .build();
    }

    /** Hedef/gerçekleşen ciro ve kâr çifti (tek hesaplama, çok tüketici). */
    private record HedefVerisi(BigDecimal hedefCiro, BigDecimal gerceklesenCiro,
                               BigDecimal hedefKar, BigDecimal gerceklesenKar) {
    }

    /** Kasa + banka bakiyeleri. */
    private record KasaBanka(BigDecimal kasa, BigDecimal banka) {
    }

    // Hangi istegin verisinin eksik kaldigini izlemek icin istek-bazli bayrak.
    private final ThreadLocal<Boolean> veriEksikTl = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private <T> T safeGet(SafeSupplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            veriEksikTl.set(Boolean.TRUE);
            log.warn("Dashboard verisi alinamadi (varsayilan kullanilacak): {}", e.getMessage());
            return defaultValue;
        }
    }

    private <T> List<T> safeGetList(SafeSupplier<List<T>> supplier, List<T> defaultValue) {
        try {
            return supplier.get();
        } catch (Exception e) {
            veriEksikTl.set(Boolean.TRUE);
            log.warn("Dashboard listesi alinamadi (varsayilan kullanilacak): {}", e.getMessage());
            return defaultValue;
        }
    }

    @FunctionalInterface
    private interface SafeSupplier<T> {
        T get();
    }
}
