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

        return RaporDTO.KdvRaporDTO.builder()
                .toplamKdvCikis(cikisKdv).toplamKdvGiris(girisKdv)
                .kdvFarki(cikisKdv.subtract(girisKdv)).build();
    }

    public List<RaporDTO.YaslandirmaDTO> yaslandirmaRaporu(Long sirketId) {
        LocalDate bugun = LocalDate.now();

        // Cari bazında en çok geciken faturanın gecikme günü DB'de grup bazında hesaplanır
        // (tüm fatura listesi belleğe yüklenmez).
        Map<Long, Integer> cariGecikme = new HashMap<>();
        for (Object[] row : faturaRepository.cariBazindaMaksGecikme(
                sirketId, Fatura.FaturaTur.SATIS.name(), Fatura.FaturaDurum.KESILDI.name(),
                List.of("ODENDI", "IPTAL"), bugun)) {
            if (row[0] == null || row[1] == null) continue;
            cariGecikme.put(((Number) row[0]).longValue(), ((Number) row[1]).intValue());
        }

        return cariHesapRepository.findBySirketIdOrderByAdAsc(sirketId).stream()
                .filter(c -> c.getBakiye() != null && c.getBakiye().compareTo(BigDecimal.ZERO) > 0)
                .map(c -> {
                    int gun = cariGecikme.getOrDefault(c.getId(), 0);
                    return RaporDTO.YaslandirmaDTO.builder()
                            .cariAd(c.getAd()).bakiye(c.getBakiye().abs()).gun(gun).aralik(aralik(gun)).build();
                })
                .sorted(Comparator.comparingInt(RaporDTO.YaslandirmaDTO::getGun).reversed())
                .collect(Collectors.toList());
    }

    private String aralik(int gun) {
        if (gun <= 0) return "Vadesi Gelmemiş";
        if (gun <= 30) return "0-30 Gün";
        if (gun <= 60) return "31-60 Gün";
        if (gun <= 90) return "61-90 Gün";
        return "90+ Gün";
    }

    /** Belirtilen ay (YYYY-MM) için KDV beyannameye hazırlık listesi üretir. */
    public RaporDTO.KdvBeyannameDTO kdvBeyannameGetir(String donem, Long sirketId) {
        YearMonth ay = donemAyCoz(donem);
        LocalDate bas = ay.atDay(1);
        LocalDate bit = ay.atEndOfMonth();

        List<Fatura> kesilmis = faturaRepository.findBySirketIdAndTarihBetween(sirketId, bas, bit).stream()
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

    /** Belirtilen ay (YYYY-MM) için BA (alış) veya BS (satış) bildirimi listesi üretir. */
    public RaporDTO.BaBsDTO baBsGetir(String donem, String tur, BigDecimal esik, Long sirketId) {
        YearMonth ay = donemAyCoz(donem);
        LocalDate bas = ay.atDay(1);
        LocalDate bit = ay.atEndOfMonth();
        BigDecimal limit = esik != null ? esik : new BigDecimal("5000");

        Fatura.FaturaTur faturaTur = "BA".equalsIgnoreCase(tur) ? Fatura.FaturaTur.ALIS : Fatura.FaturaTur.SATIS;
        List<RaporDTO.BaBsSatiriDTO> kayitlar = faturaRepository.basliklariTarihAraligindaGetir(sirketId, bas, bit).stream()
                .filter(f -> f.getTur() == faturaTur && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .filter(f -> f.getGenelToplam() != null && f.getGenelToplam().compareTo(limit) > 0)
                .map(f -> RaporDTO.BaBsSatiriDTO.builder()
                        .faturaNo(f.getFaturaNumarasi()).tarih(f.getTarih())
                        .cariAd(f.getCariHesap() != null ? f.getCariHesap().getAd() : null)
                        .cariVkn(f.getCariHesap() != null ? f.getCariHesap().getVergiNumarasi() : null)
                        .matrah(f.getAraToplam()).kdv(f.getKdv()).tutar(f.getGenelToplam())
                        .build())
                .sorted(Comparator.comparing(RaporDTO.BaBsSatiriDTO::getTarih))
                .collect(Collectors.toList());

        BigDecimal toplam = kayitlar.stream().map(RaporDTO.BaBsSatiriDTO::getTutar).reduce(BigDecimal.ZERO, BigDecimal::add);
        return RaporDTO.BaBsDTO.builder()
                .donem(donem).tur(faturaTur == Fatura.FaturaTur.ALIS ? "BA" : "BS")
                .esik(limit).kayitlar(kayitlar).toplamTutar(toplam).build();
    }

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
        List<Fatura> faturalar = faturaRepository.findBySirketIdAndTarihBetween(sirketId, baslangic, bitis).stream()
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
            BigDecimal hasila = f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO;
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
            LocalDate vade = f.getVadeTarihi() != null ? f.getVadeTarihi() : f.getTarih();
            if (vade != null && !vade.isBefore(bugun) && !vade.isAfter(bitis)) {
                BigDecimal tutar = f.getGenelToplam() != null ? f.getGenelToplam() : BigDecimal.ZERO;
                if (f.getTur() == Fatura.FaturaTur.SATIS) {
                    gunlukGiris.merge(vade, tutar, BigDecimal::add);
                } else if (f.getTur() == Fatura.FaturaTur.ALIS) {
                    gunlukCikis.merge(vade, tutar, BigDecimal::add);
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
}