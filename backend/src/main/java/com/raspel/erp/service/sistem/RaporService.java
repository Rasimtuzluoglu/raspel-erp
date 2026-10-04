package com.raspel.erp.service.sistem;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.finans.Hareket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.service.finans.CariHesapService;
import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.entity.sistem.Donem;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.dto.finans.HareketDTO;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.service.finans.HareketService;
import com.raspel.erp.dto.sistem.RaporDTO;
import com.raspel.erp.entity.finans.Butce;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.repository.finans.ButceRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import com.raspel.erp.dto.sistem.ButceGerceklesenDTO;
import com.raspel.erp.dto.sistem.PivotDTO;
import com.raspel.erp.repository.ticaret.PivotSatirProjeksiyon;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RaporService {

    /** Vadesi gelmemiş kova anahtarı. */
    public static final String KOVA_VADEDI_GELMEMIS = "VADEDI_GELMEMIS";
    /** Yaşlandırma kovalarının görünüm sırası (daha yeni kova önce). */
    public static final List<String> YASLANDIRMA_KOVALARI =
            List.of(KOVA_VADEDI_GELMEMIS, "GUN_0_30", "GUN_31_60", "GUN_61_90", "GUN_90_PLUS");

    private final CariHesapRepository cariHesapRepository;
    private final HareketRepository hareketRepository;
    private final FaturaRepository faturaRepository;
    private final com.raspel.erp.repository.ticaret.FaturaKalemRepository faturaKalemRepository;
    private final com.raspel.erp.repository.envanter.StokRepository stokRepository;
    private final com.raspel.erp.service.envanter.MaliyetService maliyetService;
    private final com.raspel.erp.repository.finans.KasaRepository kasaRepository;
    private final com.raspel.erp.repository.finans.BankaRepository bankaRepository;
    private final CariHesapService cariHesapService;
    private final HareketService hareketService;
    private final ButceRepository butceRepository;
    private final MasrafRepository masrafRepository;
    private final PdfRaporService pdfRaporService;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.repository.ticaret.IadeRepository iadeRepository;
    private final com.raspel.erp.repository.ticaret.IadeKalemRepository iadeKalemRepository;
    private final com.raspel.erp.repository.envanter.StokMaliyetHareketRepository stokMaliyetHareketRepository;

    /** TAMAMLANDI durumdaki iadeler (KDV/BA-BS düzeltmesi için). */
    private List<com.raspel.erp.entity.ticaret.Iade> tamamlanmisIadeler(Long sirketId, LocalDate bas, LocalDate bit) {
        return iadeRepository.findBySirketIdAndTurAndDurumAndTarihBetween(sirketId, "SATIS", "TAMAMLANDI", bas, bit);
    }

    public RaporDTO.CariEkstreDTO cariEkstreGetir(Long cariHesapId, LocalDate baslangic, LocalDate bitis) {
        CariHesap cari = cariHesapRepository.findById(cariHesapId)
                .orElseThrow(() -> new RuntimeException("Cari hesap bulunamadı"));
        tenantChecker.check(cari.getSirketId(), "Cari hesap");

        List<RaporDTO.CariEkstreSatiriDTO> satirlar = new java.util.ArrayList<>();
        // 1) Gercek cari hareketleri (borc/alacak yonu tur'e gore belirlenir)
        for (Hareket h : hareketRepository
                .findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(cariHesapId, baslangic, bitis)) {
            RaporDTO.CariEkstreSatiriDTO s = RaporDTO.CariEkstreSatiriDTO.builder()
                    .id(h.getId())
                    .tarih(h.getHareketTarihi())
                    .tur(h.getTur() != null ? h.getTur().name() : null)
                    .aciklama(h.getAciklama())
                    .build();
            borcAlacakUygula(s, h.getTutar());
            satirlar.add(s);
        }
        // 2) Kesilmis faturalar (gorunum icin sentetik satir; cari bakiye zaten guncel)
        for (Fatura f : faturaRepository.findByCariHesapIdAndDurumAndTarihBetweenOrderByTarihAscIdAsc(
                cariHesapId, Fatura.FaturaDurum.KESILDI, baslangic, bitis)) {
            boolean satis = f.getTur() == Fatura.FaturaTur.SATIS;
            RaporDTO.CariEkstreSatiriDTO s = RaporDTO.CariEkstreSatiriDTO.builder()
                    .id(f.getId() != null ? -f.getId() : null)
                    .tarih(f.getTarih())
                    .tur(satis ? "SATIS_FATURA" : "ALIS_FATURA")
                    .faturaNumarasi(f.getFaturaNumarasi())
                    .vadeTarihi(f.getVadeTarihi())
                    .aciklama("Fatura #" + f.getFaturaNumarasi()
                            + (f.getAciklama() != null && !f.getAciklama().isBlank() ? " - " + f.getAciklama() : ""))
                    .build();
            borcAlacakUygula(s, f.getGenelToplam());
            satirlar.add(s);
        }
        satirlar.sort(java.util.Comparator.comparing(RaporDTO.CariEkstreSatiriDTO::getTarih,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

        // Bakiye semantigi: pozitif = cari bize borclu. borc sutunu bakiyeyi artirir.
        BigDecimal donemSonu = cari.getBakiye() != null ? cari.getBakiye() : BigDecimal.ZERO;
        BigDecimal toplamBorc = BigDecimal.ZERO;
        BigDecimal toplamAlacak = BigDecimal.ZERO;
        for (RaporDTO.CariEkstreSatiriDTO s : satirlar) {
            if (s.getBorc() != null) toplamBorc = toplamBorc.add(s.getBorc());
            if (s.getAlacak() != null) toplamAlacak = toplamAlacak.add(s.getAlacak());
        }
        // Cari bakiye semantigi: negatif = cari bize borclu. Borc sutunu bakiyeyi
        // azaltir (cari borclanir), alacak sutunu artirir (cari oder / biz borclaniriz).
        BigDecimal netHareket = toplamAlacak.subtract(toplamBorc);
        BigDecimal donemBasi = donemSonu.subtract(netHareket);
        BigDecimal yuruyen = donemBasi;
        for (RaporDTO.CariEkstreSatiriDTO s : satirlar) {
            yuruyen = yuruyen.add(s.getAlacak() != null ? s.getAlacak() : BigDecimal.ZERO)
                    .subtract(s.getBorc() != null ? s.getBorc() : BigDecimal.ZERO);
            s.setYuruyenBakiye(yuruyen);
        }

        return RaporDTO.CariEkstreDTO.builder()
                .cariAd(cari.getAd())
                .cariVergiNo(cari.getVergiNumarasi())
                .cariTelefon(cari.getTelefon())
                .cariEmail(cari.getEmail())
                .cariAdres(cari.getAdres())
                .donemBasBakiye(donemBasi)
                .donemSonBakiye(donemSonu)
                .toplamBorc(toplamBorc)
                .toplamAlacak(toplamAlacak)
                .netHareket(netHareket)
                .hareketler(satirlar).build();
    }

    /** Tur'e gore borc/alacak sutunlarini doldurur (borc bakiyeyi artirir). */
    private void borcAlacakUygula(RaporDTO.CariEkstreSatiriDTO s, BigDecimal tutar) {
        BigDecimal t = tutar != null ? tutar : BigDecimal.ZERO;
        String tur = s.getTur() != null ? s.getTur() : "";
        switch (tur) {
            case "SATIS_FATURA", "ODEME", "BORC" -> { s.setBorc(t); s.setAlacak(BigDecimal.ZERO); }
            case "TAHSILAT", "ALIS_FATURA" -> { s.setBorc(BigDecimal.ZERO); s.setAlacak(t); }
            default -> { s.setBorc(BigDecimal.ZERO); s.setAlacak(BigDecimal.ZERO); }
        }
    }

    public RaporDTO.GelirGiderOzetDTO gelirGiderOzeti(LocalDate baslangic, LocalDate bitis, Long sirketId) {
        var hareketler = hareketRepository.findBySirketIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(sirketId, baslangic, bitis);

        BigDecimal toplamGelir = hareketler.stream()
                .filter(h -> h.getTur() == Hareket.HareketTuru.TAHSILAT)
                .map(h -> h.getTutar()).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal toplamGider = hareketler.stream()
                .filter(h -> h.getTur() == Hareket.HareketTuru.ODEME)
                .map(h -> h.getTutar()).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal net = toplamGelir.subtract(toplamGider);

        Map<String, BigDecimal> aylik = new LinkedHashMap<>();
        for (var h : hareketler) {
            // Yalnızca nakit akışı hareketleri (tahsilat/ödeme); BORC bir nakit hareketi değildir.
            if (h.getTur() != Hareket.HareketTuru.TAHSILAT && h.getTur() != Hareket.HareketTuru.ODEME) continue;
            String ay = h.getHareketTarihi().getYear() + "-" + String.format("%02d", h.getHareketTarihi().getMonthValue());
            BigDecimal ek = h.getTur() == Hareket.HareketTuru.TAHSILAT ? h.getTutar() : h.getTutar().negate();
            aylik.merge(ay, ek, BigDecimal::add);
        }

        List<Map<String, Object>> aylikDagilim = aylik.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ay", e.getKey());
            m.put("net", e.getValue());
            return m;
        }).collect(Collectors.toList());

        return RaporDTO.GelirGiderOzetDTO.builder()
                .toplamGelir(toplamGelir).toplamGider(toplamGider).netKarZarar(net)
                .aylikDagilim(aylikDagilim).build();
    }

    public RaporDTO.KdvRaporDTO kdvRaporu(LocalDate baslangic, LocalDate bitis, Long sirketId) {
        List<Fatura> faturalar = faturaRepository.basliklariTarihAraligindaGetir(sirketId, baslangic, bitis).stream()
                .filter(f -> f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .collect(Collectors.toList());

        BigDecimal cikisKdv = faturalar.stream()
                .filter(f -> f.getTur() == Fatura.FaturaTur.SATIS)
                .map(Fatura::getKdv).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal girisKdv = faturalar.stream()
                .filter(f -> f.getTur() == Fatura.FaturaTur.ALIS)
                .map(Fatura::getKdv).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Satış iadeleri çıkış KDV'yi, alış iadeleri giriş KDV'yi azaltır.
        BigDecimal iadeSatisKdv = iadeKdv(sirketId, "SATIS", baslangic, bitis);
        BigDecimal iadeAlisKdv = iadeKdv(sirketId, "ALIS", baslangic, bitis);
        cikisKdv = cikisKdv.subtract(iadeSatisKdv);
        girisKdv = girisKdv.subtract(iadeAlisKdv);

        return RaporDTO.KdvRaporDTO.builder()
                .toplamKdvCikis(cikisKdv).toplamKdvGiris(girisKdv)
                .kdvFarki(cikisKdv.subtract(girisKdv)).build();
    }

    /** TAMAMLANDI durumdaki iadelerin kalemlerinden toplam KDV tutarını hesaplar. */
    private BigDecimal iadeKdv(Long sirketId, String tur, LocalDate bas, LocalDate bit) {
        List<com.raspel.erp.entity.ticaret.Iade> iadeler = iadeRepository
                .findBySirketIdAndTurAndDurumAndTarihBetween(sirketId, tur, "TAMAMLANDI", bas, bit);
        if (iadeler.isEmpty()) return BigDecimal.ZERO;
        List<Long> idler = iadeler.stream().map(com.raspel.erp.entity.ticaret.Iade::getId).collect(Collectors.toList());
        return iadeKalemRepository.findByIadeIdIn(idler).stream()
                .map(k -> {
                    BigDecimal oran = k.getKdvOrani() != null ? k.getKdvOrani() : BigDecimal.ZERO;
                    return com.raspel.erp.util.FaturaTutar
                            .satir(k.getBirimFiyat(), k.getMiktar(), BigDecimal.ZERO, oran).kdv();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Vade yaşlandırma raporu (cari bazında kova matrisi).
     *
     * <p>Tutar her bir faturanın kendi vadesine göre hesaplanır: vadesi geçen
     * fatura gecikme günüyle uygun kovaya girer, vadesi gelmemiş olanlar ilk
     * kovaya. Böylece satır toplamı, o carinin faturalardan gelen toplam
     * tahsil edilecek tutarıdır.
     *
     * @param referansTarih gecikme günlerinin hesaplanacağı tarih; {@code null} ise bugün
     */
    public RaporDTO.YaslandirmaRaporDTO yaslandirmaRaporu(Long sirketId, LocalDate referansTarih) {
        LocalDate bugun = referansTarih != null ? referansTarih : LocalDate.now();
        Fatura.FaturaTur tur = Fatura.FaturaTur.SATIS;
        Fatura.FaturaDurum durum = Fatura.FaturaDurum.KESILDI;
        List<String> kapaliOdeme = List.of("ODENDI", "IPTAL");

        // Kovalama Java tarafında yapılır (native CASE ... GROUP BY sorgusu
        // PostgreSQL'de "must appear in the GROUP BY clause" hatası veriyordu).
        // Satır: [cariId, vadeTarihi, kalanTutar]
        Map<Long, Map<String, BigDecimal>> cariKovalar = new HashMap<>();
        Map<Long, int[]> cariMaksGecikme = new HashMap<>();
        Map<Long, long[]> cariGecikmeToplam = new HashMap<>();
        Map<Long, Integer> cariGecikmeAdet = new HashMap<>();

        for (Object[] row : faturaRepository.acikFaturalarVadeIcin(sirketId, tur, durum, kapaliOdeme)) {
            if (row[0] == null) continue;
            long cariId = ((Number) row[0]).longValue();
            LocalDate vade = row[1] instanceof LocalDate ld ? ld : null;
            BigDecimal kalan = bigDecimalDeger(row[2]);
            // Vadesi null olan fatura: gecikmeyi kanıtlayacak vade bilgisi yok,
            // bu yüzden "vadesi gelmemiş" kovasına girer (mutabakat korunur).
            int gecikmeGun = vade == null ? 0 : (int) ChronoUnit.DAYS.between(vade, bugun);
            String kova = yaslandirmaKovasi(gecikmeGun);

            cariKovalar.computeIfAbsent(cariId, k -> new LinkedHashMap<>()).merge(kova, kalan, BigDecimal::add);

            cariMaksGecikme.merge(cariId, new int[]{Math.max(gecikmeGun, 0)}, (a, b) -> new int[]{Math.max(a[0], b[0])});
            if (gecikmeGun > 0) {
                cariGecikmeToplam.merge(cariId, new long[]{gecikmeGun}, (a, b) -> new long[]{a[0] + b[0]});
                cariGecikmeAdet.merge(cariId, 1, Integer::sum);
            }
        }

        // Cari adları tek sorguda; N+1 sorgu yapmamak için liste halinde çekilir.
        Map<Long, String> cariAdlari = new HashMap<>();
        for (CariHesap c : cariHesapRepository.findBySirketIdOrderByAdAsc(sirketId)) {
            if (c.getId() != null) cariAdlari.put(c.getId(), c.getAd());
        }

        List<RaporDTO.YaslandirmaDTO> satirlar = new ArrayList<>();
        Map<String, BigDecimal> toplamKovalar = new LinkedHashMap<>();
        for (String kova : YASLANDIRMA_KOVALARI) toplamKovalar.put(kova, BigDecimal.ZERO);

        for (Map.Entry<Long, Map<String, BigDecimal>> entry : cariKovalar.entrySet()) {
            Long cariId = entry.getKey();
            Map<String, BigDecimal> kovalar = new LinkedHashMap<>();
            for (String kova : YASLANDIRMA_KOVALARI) {
                BigDecimal tutar = entry.getValue().getOrDefault(kova, BigDecimal.ZERO);
                kovalar.put(kova, tutar);
                toplamKovalar.merge(kova, tutar, BigDecimal::add);
            }

            BigDecimal toplam = kovalar.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gecikmis = toplam.subtract(kovalar.get(KOVA_VADEDI_GELMEMIS));
            long adet = cariGecikmeAdet.getOrDefault(cariId, 0);
            double ortalama = adet > 0
                    ? yuvarla((double) cariGecikmeToplam.getOrDefault(cariId, new long[]{0L})[0] / adet)
                    : 0d;

            satirlar.add(RaporDTO.YaslandirmaDTO.builder()
                    .cariHesapId(cariId)
                    .cariAd(cariAdlari.getOrDefault(cariId, "-"))
                    .kovalar(kovalar)
                    .toplam(toplam)
                    .enFazlaGecikmeGun(cariMaksGecikme.getOrDefault(cariId, new int[]{0})[0])
                    .ortalamaGecikmeGun(ortalama)
                    .gecikmisTutar(gecikmis)
                    .build());
        }

        // En çok geciken cari en üstte; aynı gecikmede tutar büyüğü önce.
        satirlar.sort(Comparator.comparingInt(RaporDTO.YaslandirmaDTO::getEnFazlaGecikmeGun)
                .thenComparing(RaporDTO.YaslandirmaDTO::getToplam, Comparator.reverseOrder())
                .reversed());

        BigDecimal toplamGenel = toplamKovalar.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        RaporDTO.YaslandirmaOzetDTO ozet = RaporDTO.YaslandirmaOzetDTO.builder()
                .kovalar(toplamKovalar)
                .toplam(toplamGenel)
                .gecikmisTutar(toplamGenel.subtract(toplamKovalar.get(KOVA_VADEDI_GELMEMIS)))
                .cariSayisi(satirlar.size())
                .kovaSirasi(List.copyOf(YASLANDIRMA_KOVALARI))
                .build();

        return RaporDTO.YaslandirmaRaporDTO.builder()
                .satirlar(satirlar)
                .ozet(ozet)
                .referansTarih(bugun)
                .build();
    }

    private static double yuvarla(double d) {
        return Math.round(d * 100d) / 100d;
    }

    /** Gecikme gününe göre yaşlandırma kovası. */
    static String yaslandirmaKovasi(int gecikmeGun) {
        if (gecikmeGun <= 0) return KOVA_VADEDI_GELMEMIS;
        if (gecikmeGun <= 30) return "GUN_0_30";
        if (gecikmeGun <= 60) return "GUN_31_60";
        if (gecikmeGun <= 90) return "GUN_61_90";
        return "GUN_90_PLUS";
    }

    /**
     * Native sorgu {@code SUM(...)} sonucunu sürüme göre {@link BigDecimal} ya da
     * {@link Double} döndürebiliyor; her iki durumda da BigDecimal'e çevirir.
     */
    private static BigDecimal bigDecimalDeger(Object deger) {
        if (deger == null) return BigDecimal.ZERO;
        if (deger instanceof BigDecimal bd) return bd;
        if (deger instanceof Integer i) return BigDecimal.valueOf(i);
        if (deger instanceof Long l) return BigDecimal.valueOf(l);
        return new BigDecimal(String.valueOf(deger));
    }

    /** Belirtilen ay (YYYY-MM) için KDV beyannameye hazırlık listesi üretir. */
    public RaporDTO.KdvBeyannameDTO kdvBeyannameGetir(String donem, Long sirketId) {
        YearMonth ay = donemAyCoz(donem);
        LocalDate bas = ay.atDay(1);
        LocalDate bit = ay.atEndOfMonth();

        List<Fatura> kesilmis = faturaRepository.findBySirketIdAndTarihBetweenKalemli(sirketId, bas, bit).stream()
                .filter(f -> f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .collect(Collectors.toList());

        Map<BigDecimal, BigDecimal[]> satisMap = new TreeMap<>();
        Map<BigDecimal, BigDecimal[]> alisMap = new TreeMap<>();

        for (Fatura f : kesilmis) {
            // kalemler fatura sorgusunda EntityGraph ile eager yüklenir; ek sorgu gerekmez.
            for (FaturaKalem k : f.getKalemler()) {
                BigDecimal oran = k.getKdvOrani() != null ? k.getKdvOrani() : BigDecimal.ZERO;
                BigDecimal matrah = kdvMatrah(k.getTutar(), oran);
                Map<BigDecimal, BigDecimal[]> hedef = f.getTur() == Fatura.FaturaTur.SATIS ? satisMap : alisMap;
                BigDecimal[] dizi = hedef.computeIfAbsent(oran, o -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                dizi[0] = dizi[0].add(matrah);
                dizi[1] = dizi[1].add(matrah.multiply(oran).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
        }

        // İadeler beyannameyi düzeltir: satış iadesi satış matrah/KDV'sinden, alış iadesi alıştan düşülür.
        iadeBeyannameUygula(satisMap, sirketId, "SATIS", bas, bit);
        iadeBeyannameUygula(alisMap, sirketId, "ALIS", bas, bit);

        List<RaporDTO.KdvBeyannameSatiriDTO> satislar = satisMap.entrySet().stream()
                .map(e -> RaporDTO.KdvBeyannameSatiriDTO.builder().kdvOrani(e.getKey()).matrah(e.getValue()[0]).kdv(e.getValue()[1]).build())
                .collect(Collectors.toList());
        List<RaporDTO.KdvBeyannameSatiriDTO> alislar = alisMap.entrySet().stream()
                .map(e -> RaporDTO.KdvBeyannameSatiriDTO.builder().kdvOrani(e.getKey()).matrah(e.getValue()[0]).kdv(e.getValue()[1]).build())
                .collect(Collectors.toList());

        BigDecimal hesaplanan = satislar.stream().map(RaporDTO.KdvBeyannameSatiriDTO::getKdv).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal indirilecek = alislar.stream().map(RaporDTO.KdvBeyannameSatiriDTO::getKdv).reduce(BigDecimal.ZERO, BigDecimal::add);

        return RaporDTO.KdvBeyannameDTO.builder()
                .donem(donem).satislar(satislar).alislar(alislar)
                .toplamHesaplananKdv(hesaplanan).toplamIndirilecekKdv(indirilecek)
                .odenecekKdv(hesaplanan.compareTo(indirilecek) > 0 ? hesaplanan.subtract(indirilecek) : BigDecimal.ZERO)
                .devredenKdv(indirilecek.compareTo(hesaplanan) > 0 ? indirilecek.subtract(hesaplanan) : BigDecimal.ZERO)
                .build();
    }

    /**
     * TAMAMLANDI iadelerin KDV-dahil kalem tutarlarını oran bazında matrah/KDV'ye ayrıştırıp
     * ilgili haritadan (satış/alış) düşer. Böylece beyanname iadeyi de yansıtır.
     */
    private void iadeBeyannameUygula(Map<BigDecimal, BigDecimal[]> hedefMap, Long sirketId,
                                     String tur, LocalDate bas, LocalDate bit) {
        List<com.raspel.erp.entity.ticaret.Iade> iadeler = iadeRepository
                .findBySirketIdAndTurAndDurumAndTarihBetween(sirketId, tur, "TAMAMLANDI", bas, bit);
        if (iadeler.isEmpty()) return;
        List<Long> idler = iadeler.stream().map(com.raspel.erp.entity.ticaret.Iade::getId).collect(Collectors.toList());
        for (com.raspel.erp.entity.ticaret.IadeKalem k : iadeKalemRepository.findByIadeIdIn(idler)) {
            BigDecimal oran = k.getKdvOrani() != null ? k.getKdvOrani() : BigDecimal.ZERO;
            com.raspel.erp.util.FaturaTutar.Satir satir = com.raspel.erp.util.FaturaTutar
                    .satir(k.getBirimFiyat(), k.getMiktar(), BigDecimal.ZERO, oran);
            BigDecimal[] dizi = hedefMap.computeIfAbsent(oran, o -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            dizi[0] = dizi[0].subtract(satir.net());
            dizi[1] = dizi[1].subtract(satir.kdv());
        }
    }

    /** Belirtilen ay (YYYY-MM) için BA (alış) veya BS (satış) bildirimi listesi üretir. */
    public RaporDTO.BaBsDTO baBsGetir(String donem, String tur, BigDecimal esik, Long sirketId) {        YearMonth ay = donemAyCoz(donem);
        LocalDate bas = ay.atDay(1);
        LocalDate bit = ay.atEndOfMonth();
        BigDecimal limit = esik != null ? esik : new BigDecimal("5000");

        Fatura.FaturaTur faturaTur = "BA".equalsIgnoreCase(tur) ? Fatura.FaturaTur.ALIS : Fatura.FaturaTur.SATIS;
        List<RaporDTO.BaBsSatiriDTO> kayitlar = new java.util.ArrayList<>(faturaRepository
                .basliklariTarihAraligindaGetir(sirketId, bas, bit).stream()
                .filter(f -> f.getTur() == faturaTur && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                // BA/BS eşiği KDV hariç matrah üzerinden uygulanır.
                .filter(f -> f.getAraToplam() != null && f.getAraToplam().compareTo(limit) > 0)
                .map(f -> RaporDTO.BaBsSatiriDTO.builder()
                        .faturaNo(f.getFaturaNumarasi()).tarih(f.getTarih())
                        .cariAd(f.getCariHesap() != null ? f.getCariHesap().getAd() : null)
                        .cariVkn(f.getCariHesap() != null ? f.getCariHesap().getVergiNumarasi() : null)
                        .matrah(f.getAraToplam()).kdv(f.getKdv()).tutar(f.getGenelToplam())
                        .build())
                .collect(Collectors.toList()));

        // TAMAMLANDI iadeler BA/BS'e negatif satır olarak eklenir (bildirim tutarını azaltır).
        String iadeTur = faturaTur == Fatura.FaturaTur.ALIS ? "ALIS" : "SATIS";
        List<com.raspel.erp.entity.ticaret.Iade> iadeler = iadeRepository
                .findBySirketIdAndTurAndDurumAndTarihBetween(sirketId, iadeTur, "TAMAMLANDI", bas, bit);

        // N+1 onlemi: iade kalemleri ve iade bagli faturalarin cari bilgileri
        // tek sorguda toplu cozulur. Kalemler ve cariler iade basina ayri
        // findByIadeId/findById ile cekilirse sorgu sayisi iade sayisiyla artar.
        List<Long> iadeIdleri = iadeler.stream().map(com.raspel.erp.entity.ticaret.Iade::getId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, List<com.raspel.erp.entity.ticaret.IadeKalem>> iadeKalemHaritasi = iadeIdleri.isEmpty()
                ? Map.of()
                : iadeKalemRepository.findByIadeIdIn(iadeIdleri).stream()
                        .collect(Collectors.groupingBy(com.raspel.erp.entity.ticaret.IadeKalem::getIadeId));

        List<Long> iadeFaturaIdleri = iadeler.stream().map(com.raspel.erp.entity.ticaret.Iade::getFaturaId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Fatura> iadeFaturalari = iadeFaturaIdleri.isEmpty()
                ? Map.of()
                : faturaRepository.findAllById(iadeFaturaIdleri).stream()
                        .collect(Collectors.toMap(Fatura::getId, f -> f, (a, b) -> a));
        Set<Long> iadeCariIdleri = iadeFaturalari.values().stream()
                .map(Fatura::getCariHesap).filter(java.util.Objects::nonNull)
                .map(CariHesap::getId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CariHesap> iadeCariler = iadeCariIdleri.isEmpty()
                ? Map.of()
                : cariHesapRepository.findAllById(iadeCariIdleri).stream()
                        .collect(Collectors.toMap(CariHesap::getId, c -> c, (a, b) -> a));
        Map<Long, IadeCari> iadeCarileri = new java.util.HashMap<>();
        for (Fatura f : iadeFaturalari.values()) {
            if (f.getCariHesap() == null) continue;
            CariHesap c = iadeCariler.get(f.getCariHesap().getId());
            if (c != null) {
                iadeCarileri.put(f.getId(), new IadeCari(c.getAd(), c.getVergiNumarasi()));
            }
        }

        for (com.raspel.erp.entity.ticaret.Iade iade : iadeler) {
            BigDecimal tutar = iade.getTutar() != null ? iade.getTutar() : BigDecimal.ZERO;
            if (tutar.abs().compareTo(limit) <= 0) continue;
            String cariAd = null;
            String cariVkn = null;
            BigDecimal matrahToplam = BigDecimal.ZERO;
            BigDecimal kdvToplam = BigDecimal.ZERO;
            for (com.raspel.erp.entity.ticaret.IadeKalem k : iadeKalemHaritasi.getOrDefault(iade.getId(), List.of())) {
                BigDecimal oran = k.getKdvOrani() != null ? k.getKdvOrani() : BigDecimal.ZERO;
                com.raspel.erp.util.FaturaTutar.Satir satir = com.raspel.erp.util.FaturaTutar
                        .satir(k.getBirimFiyat(), k.getMiktar(), BigDecimal.ZERO, oran);
                matrahToplam = matrahToplam.add(satir.net());
                kdvToplam = kdvToplam.add(satir.kdv());
            }
            IadeCari iadeCari = iadeCarileri.get(iade.getFaturaId());
            if (iadeCari != null) {
                cariAd = iadeCari.ad();
                cariVkn = iadeCari.vkn();
            }
            kayitlar.add(RaporDTO.BaBsSatiriDTO.builder()
                    .faturaNo("İADE #" + iade.getId()).tarih(iade.getTarih())
                    .cariAd(cariAd).cariVkn(cariVkn)
                    .matrah(matrahToplam.negate()).kdv(kdvToplam.negate()).tutar(tutar.negate())
                    .build());
        }
        kayitlar.sort(Comparator.comparing(RaporDTO.BaBsSatiriDTO::getTarih));

        BigDecimal toplam = kayitlar.stream().map(RaporDTO.BaBsSatiriDTO::getTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
        return RaporDTO.BaBsDTO.builder()
                .donem(donem).tur(faturaTur == Fatura.FaturaTur.ALIS ? "BA" : "BS")
                .esik(limit).kayitlar(kayitlar).toplamTutar(toplam).build();
    }

    /** İade satırının cari ad/vergi numarası (iade bağlı faturadan türetilir). */
    private record IadeCari(String ad, String vkn) {}

    /** Dönem (YYYY-MM) değerini çözer; geçersizse anlamlı bir iş kuralı hatası fırlatır. */
    private YearMonth donemAyCoz(String donem) {
        try {
            return YearMonth.parse(donem);
        } catch (java.time.format.DateTimeParseException | NullPointerException e) {
            throw new com.raspel.erp.exception.BusinessException("Geçersiz dönem. YYYY-MM biçiminde olmalıdır (ör. 2026-09).");
        }
    }

    private BigDecimal kdvMatrah(BigDecimal kdvliTutar, BigDecimal oran) {
        if (kdvliTutar == null) return BigDecimal.ZERO;
        return kdvliTutar.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(100).add(oran), 2, RoundingMode.HALF_UP);
    }

    /**
     * Cari karlılık raporu: her cari hesabın belirtilen dönemdeki SATIS faturalarından
     * elde ettiği hasılatı, satılan kalemlerin maliyetini ve kârını hesaplar.
     * Maliyet, kalemin bağlı olduğu stoğun tedarikçi fiyatı (yoksa alış fiyatı) üzerinden hesaplanır.
     */
    public RaporDTO.CariKarlilikDTO cariKarlilikRaporu(LocalDate baslangic, LocalDate bitis, Long sirketId) {
        List<Fatura> faturalar = faturaRepository.findBySirketIdAndTarihBetweenKalemli(sirketId, baslangic, bitis).stream()
                .filter(f -> f.getTur() == Fatura.FaturaTur.SATIS)
                .filter(f -> f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .collect(Collectors.toList());

        // Kârlılık maliyet hesabı için stok maliyetleri tek seferde toplu yüklenir (N+1 önlenir).
        Map<Long, BigDecimal> stokMaliyet = stokMaliyetleriniTopluYukle(faturalar);

        Map<Long, RaporDTO.CariKarlilikSatiriDTO> satirMap = new LinkedHashMap<>();
        for (Fatura f : faturalar) {
            Long cariId = f.getCariHesap() != null ? f.getCariHesap().getId() : null;
            String cariAd = f.getCariHesap() != null ? f.getCariHesap().getAd() : "Genel";

            BigDecimal faturaMaliyet = BigDecimal.ZERO;
            for (FaturaKalem k : f.getKalemler()) {
                BigDecimal birimMaliyet = k.getBirimMaliyet() != null
                        ? k.getBirimMaliyet()
                        : (k.getStokId() != null ? stokMaliyet.getOrDefault(k.getStokId(), BigDecimal.ZERO) : BigDecimal.ZERO);
                faturaMaliyet = faturaMaliyet.add(birimMaliyet.multiply(k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO));
            }

            RaporDTO.CariKarlilikSatiriDTO satir = satirMap.get(cariId);
            if (satir == null) {
                satir = RaporDTO.CariKarlilikSatiriDTO.builder()
                        .cariId(cariId).cariAd(cariAd)
                        .toplamSatis(BigDecimal.ZERO).toplamMaliyet(BigDecimal.ZERO)
                        .kar(BigDecimal.ZERO).karMarji(BigDecimal.ZERO).faturaSayisi(0)
                        .build();
                satirMap.put(cariId, satir);
            }
            // Hasılat için KDV hariç matrah kullanılır (genelToplam KDV dahildir).
            BigDecimal hasila = f.getAraToplam() != null ? f.getAraToplam() : BigDecimal.ZERO;
            satir.setToplamSatis(satir.getToplamSatis().add(hasila));
            satir.setToplamMaliyet(satir.getToplamMaliyet().add(faturaMaliyet));
            satir.setFaturaSayisi(satir.getFaturaSayisi() + 1);
        }

        List<RaporDTO.CariKarlilikSatiriDTO> satirlar = satirMap.values().stream()
                .peek(s -> {
                    s.setKar(s.getToplamSatis().subtract(s.getToplamMaliyet()));
                    if (s.getToplamSatis().compareTo(BigDecimal.ZERO) > 0) {
                        s.setKarMarji(s.getKar().multiply(BigDecimal.valueOf(100))
                                .divide(s.getToplamSatis(), 2, RoundingMode.HALF_UP));
                    }
                })
                .sorted(Comparator.comparing(RaporDTO.CariKarlilikSatiriDTO::getKar).reversed())
                .collect(Collectors.toList());

        BigDecimal toplamSatis = satirlar.stream().map(RaporDTO.CariKarlilikSatiriDTO::getToplamSatis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal toplamMaliyet = satirlar.stream().map(RaporDTO.CariKarlilikSatiriDTO::getToplamMaliyet)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal toplamKar = toplamSatis.subtract(toplamMaliyet);

        return RaporDTO.CariKarlilikDTO.builder()
                .toplamSatis(toplamSatis).toplamMaliyet(toplamMaliyet)
                .toplamKar(toplamKar).satirlar(satirlar)
                .build();
    }

    private BigDecimal stokMaliyetGetir(Long stokId) {
        return stokRepository.findById(stokId)
                .map(s -> {
                    BigDecimal m = maliyetService.ortalamaMaliyet(s);
                    if (m == null || m.signum() == 0) {
                        m = s.getTedarikciFiyat() != null ? s.getTedarikciFiyat() : s.getFiyat();
                    }
                    return m != null ? m : BigDecimal.ZERO;
                })
                .orElse(BigDecimal.ZERO);
    }

    /** Verilen faturalardaki tüm stokların maliyetini tek sorguda toplu hesaplar. */
    private Map<Long, BigDecimal> stokMaliyetleriniTopluYukle(List<Fatura> faturalar) {
        Set<Long> stokIdler = new HashSet<>();
        for (Fatura f : faturalar) {
            for (FaturaKalem k : f.getKalemler()) {
                if (k.getStokId() != null && k.getBirimMaliyet() == null) {
                    stokIdler.add(k.getStokId());
                }
            }
        }
        if (stokIdler.isEmpty()) return Map.of();

        Map<Long, BigDecimal> sonuc = new HashMap<>();
        for (com.raspel.erp.entity.envanter.Stok s : stokRepository.findAllById(stokIdler)) {
            BigDecimal m = maliyetService.ortalamaMaliyet(s);
            if (m == null || m.signum() == 0) {
                m = s.getTedarikciFiyat() != null ? s.getTedarikciFiyat() : s.getFiyat();
            }
            sonuc.put(s.getId(), m != null ? m : BigDecimal.ZERO);
        }
        return sonuc;
    }

    /**
     * Ürün bazlı kârlılık raporu: her ürünün alış maliyeti (fiyat), satış fiyatı ve kâr marjını getirir.
     */
    public List<com.raspel.erp.dto.sistem.UrunKarlilikDTO> urunKarlilikRaporu(Long sirketId) {
        if (sirketId == null) {
            return List.of();
        }
        List<com.raspel.erp.entity.envanter.Stok> stoklar =
                stokRepository.findBySirketIdOrderByAd(sirketId);

        return stoklar.stream().map(s -> {
            BigDecimal alis = s.getFiyat() != null ? s.getFiyat() : BigDecimal.ZERO;
            BigDecimal satis = s.getSatisFiyati() != null ? s.getSatisFiyati() : BigDecimal.ZERO;
            BigDecimal kar = satis.subtract(alis);
            BigDecimal marj = BigDecimal.ZERO;
            if (satis.compareTo(BigDecimal.ZERO) > 0) {
                marj = kar.multiply(BigDecimal.valueOf(100)).divide(satis, 2, RoundingMode.HALF_UP);
            }
            return com.raspel.erp.dto.sistem.UrunKarlilikDTO.builder()
                    .stokId(s.getId())
                    .stokKodu(s.getStokKodu())
                    .stokAd(s.getAd())
                    .alisFiyat(alis)
                    .satisFiyati(satis)
                    .kar(kar)
                    .karMarji(marj)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Tedarikçi bazlı ürün raporu: hangi tedarikçiden hangi ürünler geldi (toplam miktar, son fiyat, son tarih).
     */
    public List<com.raspel.erp.dto.sistem.TedarikciUrunDTO> tedarikciUrunRaporu(Long sirketId) {
        List<com.raspel.erp.repository.ticaret.TedarikciUrunProjeksiyon> projeksiyonlar =
                faturaKalemRepository.tedarikciUrunler(sirketId, Fatura.FaturaTur.ALIS, Fatura.FaturaDurum.KESILDI);

        List<Long> stokIdler = projeksiyonlar.stream()
                .map(com.raspel.erp.repository.ticaret.TedarikciUrunProjeksiyon::getStokId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, com.raspel.erp.entity.envanter.Stok> stokMap = stokIdler.isEmpty() ? Map.of()
                : stokRepository.findAllById(stokIdler).stream()
                        .collect(Collectors.toMap(com.raspel.erp.entity.envanter.Stok::getId, s -> s));

        return projeksiyonlar.stream().map(p -> {
            com.raspel.erp.entity.envanter.Stok stok = stokMap.get(p.getStokId());
            return com.raspel.erp.dto.sistem.TedarikciUrunDTO.builder()
                    .cariHesapId(p.getCariHesapId())
                    .cariHesapAd(p.getCariHesapAd())
                    .stokId(p.getStokId())
                    .stokAd(stok != null ? stok.getAd() : null)
                    .stokKodu(stok != null ? stok.getStokKodu() : null)
                    .toplamMiktar(p.getToplamMiktar())
                    .sonBirimFiyat(p.getSonBirimFiyat())
                    .sonTarih(p.getSonTarih())
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 30/60/90 Günlük Nakit Akışı Projeksiyonu
     */
    public com.raspel.erp.dto.sistem.NakitAkisiProjeksiyonDTO nakitAkisiProjeksiyonu(int gunSayisi, Long sirketId) {
        if (gunSayisi <= 0) gunSayisi = 30;

        // Mevcut Kasa + Banka başlangıç likiditesi
        BigDecimal kasaBakiye = kasaRepository.findBySirketIdOrderByAd(sirketId).stream()
                .map(k -> k.getBakiye() != null ? k.getBakiye() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal bankaBakiye = bankaRepository.findBySirketIdOrderByAd(sirketId).stream()
                .map(b -> b.getBakiye() != null ? b.getBakiye() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal baslangicBakiyesi = kasaBakiye.add(bankaBakiye);

        LocalDate bugun = LocalDate.now();
        LocalDate bitis = bugun.plusDays(gunSayisi);

        // Gelecek vadeli Satış ve Alış faturaları (yalnızca başlıklar; kalemler gerekmez)
        List<Fatura> faturalar = faturaRepository.basliklariGetir(sirketId).stream()
                .filter(f -> f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .collect(Collectors.toList());

        Map<LocalDate, BigDecimal> gunlukGiris = new HashMap<>();
        Map<LocalDate, BigDecimal> gunlukCikis = new HashMap<>();

        for (Fatura f : faturalar) {
            // Yalnızca ödenmemiş bakiye projeksiyona girer; tam ödenmiş fatura dahil edilmez.
            BigDecimal kalan = f.getKalanTutar() != null ? f.getKalanTutar() : BigDecimal.ZERO;
            if (kalan.signum() <= 0) continue;
            LocalDate vade = f.getVadeTarihi() != null ? f.getVadeTarihi() : f.getTarih();
            if (vade == null) continue;
            // Vadesi geçmiş alacaklar da bugüne (ilk gün) yansıtılır.
            LocalDate projeksiyonGun = vade.isBefore(bugun) ? bugun : vade;
            if (!projeksiyonGun.isBefore(bugun) && !projeksiyonGun.isAfter(bitis)) {
                if (f.getTur() == Fatura.FaturaTur.SATIS) {
                    gunlukGiris.merge(projeksiyonGun, kalan, BigDecimal::add);
                } else if (f.getTur() == Fatura.FaturaTur.ALIS) {
                    gunlukCikis.merge(projeksiyonGun, kalan, BigDecimal::add);
                }
            }
        }

        List<com.raspel.erp.dto.sistem.NakitAkisiProjeksiyonDTO.GunlukProjeksiyonDTO> gunlukList = new ArrayList<>();
        BigDecimal kumulatif = baslangicBakiyesi;
        BigDecimal toplamGiris = BigDecimal.ZERO;
        BigDecimal toplamCikis = BigDecimal.ZERO;

        for (int i = 0; i <= gunSayisi; i++) {
            LocalDate tarih = bugun.plusDays(i);
            BigDecimal giris = gunlukGiris.getOrDefault(tarih, BigDecimal.ZERO);
            BigDecimal cikis = gunlukCikis.getOrDefault(tarih, BigDecimal.ZERO);
            BigDecimal net = giris.subtract(cikis);
            kumulatif = kumulatif.add(net);

            toplamGiris = toplamGiris.add(giris);
            toplamCikis = toplamCikis.add(cikis);

            gunlukList.add(com.raspel.erp.dto.sistem.NakitAkisiProjeksiyonDTO.GunlukProjeksiyonDTO.builder()
                    .tarih(tarih)
                    .beklenenGiris(giris)
                    .beklenenCikis(cikis)
                    .netAkis(net)
                    .kumulatifBakiye(kumulatif)
                    .aciklama(giris.compareTo(BigDecimal.ZERO) > 0 || cikis.compareTo(BigDecimal.ZERO) > 0 ? "Vadesi gelen fatura akışı" : "Rutin dönem")
                    .build());
        }

        return com.raspel.erp.dto.sistem.NakitAkisiProjeksiyonDTO.builder()
                .baslangicBakiyesi(baslangicBakiyesi)
                .toplamBeklenenGiris(toplamGiris)
                .toplamBeklenenCikis(toplamCikis)
                .tahminiBitisBakiyesi(kumulatif)
                .projeksiyonGunu(gunSayisi)
                .gunlukAkis(gunlukList)
                .build();
    }

    /**
     * Bütçe vs Gerçekleşen raporu. Belirli bir yıl (ve isteğe bağlı ay) için
     * kategori bazlı planlanan bütçe ile gerçekleşen masrafı karşılaştırır.
     */
    @Transactional(readOnly = true)
    public List<ButceGerceklesenDTO> butceGerceklesenRaporu(Long sirketId, Integer yil, Integer ay) {
        List<Butce> butceler = butceRepository.findBySirketIdOrderByYilDescAyDesc(sirketId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        Map<String, BigDecimal> butceHaritasi = butceler.stream()
                .filter(b -> b.getYil() != null && b.getYil().equals(yil))
                .filter(b -> ay == null || (b.getAy() != null && b.getAy().equals(ay)))
                .filter(b -> b.getKategori() != null && !b.getKategori().isBlank())
                .collect(Collectors.groupingBy(Butce::getKategori,
                        Collectors.reducing(BigDecimal.ZERO, Butce::getTutar, BigDecimal::add)));

        LocalDate baslangic = ay != null
                ? LocalDate.of(yil, ay, 1)
                : LocalDate.of(yil, 1, 1);
        LocalDate bitis = ay != null
                ? YearMonth.of(yil, ay).atEndOfMonth()
                : LocalDate.of(yil, 12, 31);

        Map<String, BigDecimal> gerceklesenHaritasi = masrafRepository.findBySirketIdAndTarihBetween(sirketId, baslangic, bitis).stream()
                .filter(m -> m.getKategori() != null && !m.getKategori().isBlank())
                .collect(Collectors.groupingBy(Masraf::getKategori,
                        Collectors.reducing(BigDecimal.ZERO, Masraf::getTutar, BigDecimal::add)));

        Set<String> kategoriler = new TreeSet<>();
        kategoriler.addAll(butceHaritasi.keySet());
        kategoriler.addAll(gerceklesenHaritasi.keySet());

        List<ButceGerceklesenDTO> sonuc = new ArrayList<>();
        for (String kategori : kategoriler) {
            BigDecimal butce = butceHaritasi.getOrDefault(kategori, BigDecimal.ZERO);
            BigDecimal gerceklesen = gerceklesenHaritasi.getOrDefault(kategori, BigDecimal.ZERO);
            BigDecimal sapma = gerceklesen.subtract(butce);
            BigDecimal yuzde = butce.compareTo(BigDecimal.ZERO) > 0
                    ? gerceklesen.multiply(BigDecimal.valueOf(100)).divide(butce, 1, RoundingMode.HALF_UP)
                    : null;
            sonuc.add(ButceGerceklesenDTO.builder()
                    .kategori(kategori)
                    .butce(butce)
                    .gerceklesen(gerceklesen)
                    .sapma(sapma)
                    .kullanimYuzdesi(yuzde)
                    .build());
        }
        return sonuc;
    }

    public byte[] butceGerceklesenPdf(String[] kolonlar, List<String[]> satirlar, Integer yil, Integer ay) {
        String baslik = "Bütçe vs Gerçekleşen Raporu - " + yil + (ay != null ? "/" + ay : "");
        return pdfRaporService.tabloRaporu(baslik, kolonlar, satirlar);
    }

    /**
     * Dinamik pivot tablo. satirBoyut / sutunBoyut / degerMetrik parametrelerine göre
     * fatura kalemlerini çaprazlar ve özetler.
     */
    @Transactional(readOnly = true)
    public PivotDTO pivot(Long sirketId, String satirBoyut, String sutunBoyut, String degerMetrik,
                          LocalDate baslangic, LocalDate bitis) {
        LocalDate bas = baslangic != null ? baslangic : LocalDate.now().withDayOfMonth(1);
        LocalDate bit = bitis != null ? bitis : LocalDate.now();
        List<PivotSatirProjeksiyon> satirlar = faturaKalemRepository.pivotSatirlari(sirketId,
                Fatura.FaturaDurum.KESILDI, bas, bit);

        String satir = satirBoyut != null ? satirBoyut : "cari";
        String sutun = sutunBoyut != null ? sutunBoyut : "ay";
        String metrik = degerMetrik != null ? degerMetrik : "tutar";

        Map<String, Map<String, BigDecimal>> hucreler = new LinkedHashMap<>();
        Map<String, BigDecimal> satirToplam = new LinkedHashMap<>();
        Map<String, BigDecimal> sutunToplam = new LinkedHashMap<>();
        Set<String> sutunSet = new TreeSet<>();

        for (PivotSatirProjeksiyon p : satirlar) {
            String satirK = boyutDegeri(p, satir);
            String sutunK = boyutDegeri(p, sutun);
            BigDecimal deger = metrikDegeri(p, metrik);

            hucreler.computeIfAbsent(satirK, k -> new LinkedHashMap<>())
                    .merge(sutunK, deger, BigDecimal::add);
            satirToplam.merge(satirK, deger, BigDecimal::add);
            sutunToplam.merge(sutunK, deger, BigDecimal::add);
            sutunSet.add(sutunK);
        }

        BigDecimal genel = satirToplam.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        return PivotDTO.builder()
                .satirlar(new ArrayList<>(hucreler.keySet()))
                .sutunlar(new ArrayList<>(sutunSet))
                .hucreler(hucreler)
                .satirToplamlari(satirToplam)
                .sutunToplamlari(sutunToplam)
                .genelToplam(genel)
                .build();
    }

    private String boyutDegeri(PivotSatirProjeksiyon p, String boyut) {
        return switch (boyut) {
            case "stok" -> p.getStokAd() != null ? p.getStokAd() : (p.getStokId() != null ? "Ürün #" + p.getStokId() : "-");
            case "kategori" -> p.getKategori() != null ? p.getKategori() : "-";
            case "tur" -> p.getTur() != null ? p.getTur() : "-";
            case "odeme" -> p.getOdemeDurumu() != null ? p.getOdemeDurumu() : "-";
            case "ay" -> p.getTarih() != null ? p.getTarih().getYear() + "-" + String.format("%02d", p.getTarih().getMonthValue()) : "-";
            default -> p.getCariAd() != null ? p.getCariAd() : (p.getCariHesapId() != null ? "Cari #" + p.getCariHesapId() : "Anlık");
        };
    }

    private BigDecimal metrikDegeri(PivotSatirProjeksiyon p, String metrik) {
        if ("adet".equals(metrik)) {
            return BigDecimal.valueOf(p.getAdet() != null ? p.getAdet() : 0);
        }
        return p.getTutar() != null ? p.getTutar() : BigDecimal.ZERO;
    }

    /**
     * Stok değerleme: her stok için ağırlıklı ortalama maliyet ve FIFO (katman bazlı)
     * değer. FIFO katmanları maliyet defterindeki giriş/çıkış hareketlerinden yeniden
     * oluşturulur (giriş = katman, çıkış = eski katmanlardan tüketim).
     */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.sistem.StokAnalizDTO.Degerleme stokDegerleme(Long sirketId) {
        List<com.raspel.erp.entity.envanter.Stok> stoklar = stokRepository.findBySirketIdOrderByAd(sirketId);
        List<com.raspel.erp.dto.sistem.StokAnalizDTO.DegerlemeSatiri> satirlar = new java.util.ArrayList<>();
        BigDecimal toplamOrtalama = BigDecimal.ZERO;
        BigDecimal toplamFifo = BigDecimal.ZERO;
        for (com.raspel.erp.entity.envanter.Stok s : stoklar) {
            BigDecimal miktar = nz(s.getMiktar());
            if (miktar.signum() <= 0) continue;

            BigDecimal ortBirim = nz(maliyetService.ortalamaMaliyet(s));
            BigDecimal ortDeger = ortBirim.multiply(miktar).setScale(2, java.math.RoundingMode.HALF_UP);

            // FIFO katmanları: GIRIS katman ekler, CIKIS en eski katmandan tüketir.
            java.util.Deque<BigDecimal[]> katmanlar = new java.util.ArrayDeque<>();
            for (var m : stokMaliyetHareketRepository.findByStokIdOrderByTarihAscIdAsc(s.getId())) {
                BigDecimal mh = nz(m.getMiktar());
                if ("GIRIS".equals(m.getTur())) {
                    katmanlar.addLast(new BigDecimal[]{mh, nz(m.getBirimMaliyet())});
                } else if ("CIKIS".equals(m.getTur())) {
                    BigDecimal kalan = mh;
                    while (kalan.signum() > 0 && !katmanlar.isEmpty()) {
                        BigDecimal[] katman = katmanlar.peekFirst();
                        if (katman[0].compareTo(kalan) <= 0) {
                            kalan = kalan.subtract(katman[0]);
                            katmanlar.removeFirst();
                        } else {
                            katman[0] = katman[0].subtract(kalan);
                            kalan = BigDecimal.ZERO;
                        }
                    }
                }
            }
            BigDecimal fifoMiktar = BigDecimal.ZERO;
            BigDecimal fifoDeger = BigDecimal.ZERO;
            for (BigDecimal[] katman : katmanlar) {
                fifoMiktar = fifoMiktar.add(katman[0]);
                fifoDeger = fifoDeger.add(katman[0].multiply(katman[1]));
            }
            fifoDeger = fifoDeger.setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal fifoBirim = fifoMiktar.signum() > 0
                    ? fifoDeger.divide(fifoMiktar, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

            satirlar.add(com.raspel.erp.dto.sistem.StokAnalizDTO.DegerlemeSatiri.builder()
                    .stokId(s.getId()).stokKodu(s.getStokKodu()).ad(s.getAd())
                    .miktar(miktar)
                    .ortalamaBirimMaliyet(ortBirim.setScale(2, java.math.RoundingMode.HALF_UP))
                    .ortalamaDeger(ortDeger)
                    .fifoBirimMaliyet(fifoBirim)
                    .fifoDeger(fifoDeger)
                    .build());
            toplamOrtalama = toplamOrtalama.add(ortDeger);
            toplamFifo = toplamFifo.add(fifoDeger);
        }
        return com.raspel.erp.dto.sistem.StokAnalizDTO.Degerleme.builder()
                .toplamOrtalamaDeger(toplamOrtalama)
                .toplamFifoDeger(toplamFifo)
                .kalemSayisi(satirlar.size())
                .satirlar(satirlar)
                .build();
    }

    /** Sipariş önerisi: minimum seviyenin altındaki stoklar için hedefe (min x2) tamamlama. */
    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.sistem.StokAnalizDTO.OneriSatiri> siparisOnerisi(Long sirketId) {
        List<com.raspel.erp.dto.sistem.StokAnalizDTO.OneriSatiri> satirlar = new java.util.ArrayList<>();
        for (com.raspel.erp.entity.envanter.Stok s : stokRepository.kritikStoklar(sirketId)) {
            BigDecimal mevcut = nz(s.getMiktar());
            BigDecimal min = nz(s.getMinMiktar());
            BigDecimal hedef = min.multiply(BigDecimal.valueOf(2));
            BigDecimal oneri = hedef.subtract(mevcut);
            if (oneri.signum() <= 0) continue;
            BigDecimal birim = nz(maliyetService.ortalamaMaliyet(s));
            satirlar.add(com.raspel.erp.dto.sistem.StokAnalizDTO.OneriSatiri.builder()
                    .stokId(s.getId()).stokKodu(s.getStokKodu()).ad(s.getAd())
                    .mevcut(mevcut).minMiktar(min).hedefMiktar(hedef).oneriMiktar(oneri)
                    .birimMaliyet(birim.setScale(2, java.math.RoundingMode.HALF_UP))
                    .tahminiTutar(oneri.multiply(birim).setScale(2, java.math.RoundingMode.HALF_UP))
                    .tedarikciId(s.getTedarikciId())
                    .build());
        }
        return satirlar;
    }

    /** Satış temsilcisi performansı: cari kartındaki temsilciye göre satış toplamı. */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.sistem.StokAnalizDTO.TemsilciPerformans temsilciPerformans(
            Long sirketId, LocalDate baslangic, LocalDate bitis) {
        List<com.raspel.erp.repository.ticaret.TemsilciPerformansProjeksiyon> kayitlar =
                faturaRepository.temsilciPerformans(sirketId,
                        com.raspel.erp.entity.ticaret.Fatura.FaturaTur.SATIS,
                        com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.KESILDI,
                        baslangic, bitis);
        List<com.raspel.erp.dto.sistem.StokAnalizDTO.TemsilciSatiri> satirlar = new java.util.ArrayList<>();
        BigDecimal toplam = BigDecimal.ZERO;
        long toplamFatura = 0;
        for (var k : kayitlar) {
            BigDecimal satis = k.getToplamSatis() != null ? k.getToplamSatis() : BigDecimal.ZERO;
            long adet = k.getFaturaSayisi() != null ? k.getFaturaSayisi() : 0;
            // Temsilci atanmamış cariler ayrı satır olarak işaretlenir. Adı null
            // bırakılır ki frontend kendi i18n etiketini ("Atanmamış") kullansın;
            // burada Türkçe sabit dönmek EN kullanıcısına sızıyordu.
            boolean atanmamis = k.getTemsilciId() == null;
            satirlar.add(com.raspel.erp.dto.sistem.StokAnalizDTO.TemsilciSatiri.builder()
                    .temsilciId(k.getTemsilciId())
                    .temsilciAd(atanmamis ? null : k.getTemsilciAd())
                    .atanmamisMi(atanmamis)
                    .faturaSayisi(adet)
                    .toplamSatis(satis)
                    .ortalamaFatura(adet > 0 ? satis.divide(BigDecimal.valueOf(adet), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO)
                    .build());
            toplam = toplam.add(satis);
            toplamFatura += adet;
        }
        return com.raspel.erp.dto.sistem.StokAnalizDTO.TemsilciPerformans.builder()
                .toplamSatis(toplam)
                .toplamFatura(toplamFatura)
                .satirlar(satirlar)
                .build();
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}