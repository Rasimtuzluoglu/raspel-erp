package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.Gorunum360DTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.entity.ticaret.Teslimat;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.TeslimatRepository;
import com.raspel.erp.service.envanter.MaliyetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 360 derece özet görünümleri.
 * <ul>
 *   <li>Stok kâr 360: ürün bazında ciro, maliyet, brüt kâr ve marj.</li>
 *   <li>Çalışan performans 360: teslimat sayıları ve tutarları.</li>
 *   <li>Müşteri segmentasyonu: kural tabanlı segment + gerekçe.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class Gorunum360Service {

    private static final int OLCEK = 2;
    private static final List<String> KAPALI_DURUMLAR = List.of("TESLIM_EDILDI", "IPTAL");

    private final FaturaRepository faturaRepository;
    private final StokRepository stokRepository;
    private final CariHesapRepository cariHesapRepository;
    private final TeslimatRepository teslimatRepository;
    private final MaliyetService maliyetService;

    // ---------------- Stok Kâr 360 ----------------

    public Gorunum360DTO.StokKar stokKar360(Long sirketId, LocalDate baslangic, LocalDate bitis) {
        LocalDate bas = baslangic != null ? baslangic : LocalDate.now().withDayOfMonth(1);
        LocalDate bit = bitis != null ? bitis : LocalDate.now();

        Map<Long, Stok> stokCache = new HashMap<>();
        Map<Long, Gorunum360DTO.Satir> map = new LinkedHashMap<>();

        List<Fatura> faturalar = faturaRepository.findBySirketIdAndTarihBetweenKalemli(sirketId, bas, bit);
        for (Fatura f : faturalar) {
            if (f.getTur() != Fatura.FaturaTur.SATIS || f.getDurum() != Fatura.FaturaDurum.KESILDI) continue;
            for (FaturaKalem k : f.getKalemler()) {
                if (k.getStokId() == null) continue;
                BigDecimal adet = k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO;
                if (adet.signum() == 0) continue;
                BigDecimal netBirim = netBirim(k);
                BigDecimal ciro = netBirim.multiply(adet);
                Stok stok = stokCache.computeIfAbsent(k.getStokId(), id -> stokRepository.findById(id).orElse(null));
                BigDecimal hamMaliyet = k.getBirimMaliyet() != null && k.getBirimMaliyet().signum() > 0
                        ? k.getBirimMaliyet()
                        : (stok != null ? maliyetService.ortalamaMaliyet(stok) : BigDecimal.ZERO);
                final BigDecimal birimMaliyet = hamMaliyet != null ? hamMaliyet : BigDecimal.ZERO;
                BigDecimal maliyet = birimMaliyet.multiply(adet);

                final Long stokId = k.getStokId();
                Gorunum360DTO.Satir satir = map.get(stokId);
                if (satir == null) {
                    satir = Gorunum360DTO.Satir.builder()
                            .stokId(stokId)
                            .ad(stok != null ? stok.getAd() : "Bilinmeyen ürün")
                            .stokKodu(stok != null ? stok.getStokKodu() : null)
                            .kategori(stok != null ? stok.getKategori() : null)
                            .satisAdet(BigDecimal.ZERO).ciro(BigDecimal.ZERO).maliyet(BigDecimal.ZERO)
                            .stokMiktar(stok != null ? stok.getMiktar() : null)
                            .birimMaliyet(birimMaliyet)
                            .build();
                    map.put(stokId, satir);
                }
                satir.setSatisAdet(satir.getSatisAdet().add(adet));
                satir.setCiro(satir.getCiro().add(ciro));
                satir.setMaliyet(satir.getMaliyet().add(maliyet));
            }
        }

        List<Gorunum360DTO.Satir> liste = new ArrayList<>();
        BigDecimal toplamCiro = BigDecimal.ZERO;
        BigDecimal toplamKar = BigDecimal.ZERO;
        for (Gorunum360DTO.Satir s : map.values()) {
            BigDecimal ciro = para(s.getCiro());
            BigDecimal maliyet = para(s.getMaliyet());
            BigDecimal kar = ciro.subtract(maliyet);
            s.setCiro(ciro);
            s.setMaliyet(maliyet);
            s.setBrutKar(kar);
            s.setMarj(marj(kar, ciro));
            liste.add(s);
        }
        for (Gorunum360DTO.Satir s : liste) {
            toplamCiro = toplamCiro.add(s.getCiro());
            toplamKar = toplamKar.add(s.getBrutKar());
        }
        final BigDecimal tc = toplamCiro;
        final BigDecimal tk = toplamKar;
        liste.sort(Comparator.comparing(Gorunum360DTO.Satir::getBrutKar, Comparator.nullsLast(Comparator.reverseOrder())));

        return Gorunum360DTO.StokKar.builder()
                .urunler(liste)
                .toplamCiro(para(tc))
                .toplamKar(para(tk))
                .genelMarj(marj(tk, tc))
                .negatifMarjAdet((int) liste.stream().filter(x -> x.getBrutKar() != null && x.getBrutKar().signum() < 0).count())
                .toplamUrun(liste.size())
                .build();
    }

    // ---------------- Çalışan Performans 360 ----------------

    public Gorunum360DTO.CalisanPerformans calisanPerformans360(Long sirketId) {
        List<Teslimat> hepsi = teslimatRepository.findBySirketId(sirketId);

        Map<Long, Gorunum360DTO.Calisan> map = new LinkedHashMap<>();
        long toplam = 0, tamamlanan = 0;
        for (Teslimat t : hepsi) {
            if (t.getDriverId() == null) continue;
            Gorunum360DTO.Calisan c = map.computeIfAbsent(t.getDriverId(), id -> Gorunum360DTO.Calisan.builder()
                    .id(id)
                    .ad(t.getTeslimEdenAd() != null ? t.getTeslimEdenAd() : ("Şoför #" + id))
                    .rol("DRIVER")
                    .toplamTeslimat(0).tamamlananTeslimat(0).bekleyenTeslimat(0)
                    .teslimTutari(BigDecimal.ZERO)
                    .build());
            c.setToplamTeslimat(c.getToplamTeslimat() + 1);
            boolean tamam = "TESLIM_EDILDI".equals(t.getDurum());
            if (tamam) c.setTamamlananTeslimat(c.getTamamlananTeslimat() + 1);
            else c.setBekleyenTeslimat(c.getBekleyenTeslimat() + 1);
            toplam++;
            if (tamam) tamamlanan++;
        }
        List<Gorunum360DTO.Calisan> liste = new ArrayList<>(map.values());
        liste.sort(Comparator.comparingLong(Gorunum360DTO.Calisan::getToplamTeslimat).reversed());

        return Gorunum360DTO.CalisanPerformans.builder()
                .calisanlar(liste).toplamTeslimat(toplam).tamamlananTeslimat(tamamlanan).build();
    }

    // ---------------- Müşteri Segmentasyonu ----------------

    public Gorunum360DTO.MusteriSegment musteriSegmentasyon(Long sirketId) {
        List<CariHesap> cariler = cariHesapRepository.findBySirketIdOrderByAdAsc(sirketId);
        LocalDate simdi = LocalDate.now();
        LocalDate birYilOnce = simdi.minusDays(365);

        List<Fatura> satislar = faturaRepository.findBySirketIdAndTarihBetweenKalemli(sirketId, birYilOnce, simdi).stream()
                .filter(f -> f.getTur() == Fatura.FaturaTur.SATIS && f.getDurum() == Fatura.FaturaDurum.KESILDI)
                .collect(Collectors.toList());

        Map<Long, List<Fatura>> cariFaturalari = satislar.stream()
                .filter(f -> f.getCariHesap() != null)
                .collect(Collectors.groupingBy(f -> f.getCariHesap().getId()));

        Map<String, long[]> ozet = new LinkedHashMap<>();
        Map<String, BigDecimal[]> ozetCiro = new LinkedHashMap<>();
        List<Gorunum360DTO.MusteriSegmentSatir> satirlar = new ArrayList<>();

        for (CariHesap c : cariler) {
            List<Fatura> cf = cariFaturalari.getOrDefault(c.getId(), List.of());
            BigDecimal ciro = cf.stream().map(Fatura::getGenelToplam).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long adet = cf.size();
            BigDecimal bakiye = c.getBakiye() != null ? c.getBakiye() : BigDecimal.ZERO;
            BigDecimal kalan = cf.stream().map(Fatura::getKalanTutar).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Integer sonFaturaGun = cf.stream().map(Fatura::getTarih).filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .map(d -> (int) ChronoUnit.DAYS.between(d, simdi))
                    .orElse(null);

            SegmentSonuc seg = segmentBul(ciro, adet, kalan, sonFaturaGun, bakiye);
            satirlar.add(Gorunum360DTO.MusteriSegmentSatir.builder()
                    .cariId(c.getId())
                    .ad(c.getAd())
                    .segment(seg.segment)
                    .gerekce(seg.gerekce)
                    .ciro(para(ciro))
                    .bakiye(para(bakiye))
                    .kalanTutar(para(kalan))
                    .faturaAdet(adet)
                    .sonFaturaGunOnce(sonFaturaGun)
                    .build());

            long[] a = ozet.computeIfAbsent(seg.segment, k -> new long[]{0});
            a[0]++;
            BigDecimal[] ca = ozetCiro.computeIfAbsent(seg.segment, k -> new BigDecimal[]{BigDecimal.ZERO});
            ca[0] = ca[0].add(ciro);
        }

        satirlar.sort(Comparator.comparing(Gorunum360DTO.MusteriSegmentSatir::getCiro,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<Gorunum360DTO.SegmentOzet> ozetListe = new ArrayList<>();
        for (Map.Entry<String, long[]> e : ozet.entrySet()) {
            ozetListe.add(Gorunum360DTO.SegmentOzet.builder()
                    .segment(e.getKey())
                    .adet(e.getValue()[0])
                    .toplamCiro(para(ozetCiro.getOrDefault(e.getKey(), new BigDecimal[]{BigDecimal.ZERO})[0]))
                    .build());
        }

        return Gorunum360DTO.MusteriSegment.builder().segmentOzeti(ozetListe).musteriler(satirlar).build();
    }

    private record SegmentSonuc(String segment, String gerekce) {
    }

    /**
     * Kural tabanlı segment + gerekçe.
     * Öncelik: PASIF > RISKLI > VIP > DUZENLI > YENI > GELISMEDE.
     */
    private SegmentSonuc segmentBul(BigDecimal ciro, long adet, BigDecimal kalan, Integer sonGun, BigDecimal bakiye) {
        BigDecimal vipCiro = BigDecimal.valueOf(100000);
        BigDecimal duzenliCiro = BigDecimal.valueOf(25000);

        if (adet > 0 && sonGun != null && sonGun > 180) {
            return new SegmentSonuc("PASIF",
                    "Son 6 aydır (" + sonGun + " gün) satış yok");
        }
        if (kalan.signum() > 0 && kalan.compareTo(BigDecimal.valueOf(10000)) > 0) {
            return new SegmentSonuc("RISKLI",
                    "Ödenmemiş " + kalan.setScale(0, RoundingMode.HALF_UP) + " ₺ alacak var");
        }
        if (ciro.compareTo(vipCiro) >= 0) {
            return new SegmentSonuc("VIP",
                    "Yıllık ciro " + ciro.setScale(0, RoundingMode.HALF_UP) + " ₺ (≥100.000 ₺)");
        }
        if (ciro.compareTo(duzenliCiro) >= 0) {
            return new SegmentSonuc("DUZENLI",
                    "Yıllık ciro " + ciro.setScale(0, RoundingMode.HALF_UP) + " ₺, " + adet + " fatura");
        }
        if (adet > 0) {
            return new SegmentSonuc("YENI", adet + " fatura, henüz düzenli ciro eşiğinde değil");
        }
        if (bakiye.signum() > 0) {
            return new SegmentSonuc("GELISMEDE", "Kayıtlı ancak henüz satış yok, alacak bakiye mevcut");
        }
        return new SegmentSonuc("GELISMEDE", "Kayıtlı, henüz satış yok");
    }

    // ---------------- yardımcılar ----------------

    private BigDecimal netBirim(FaturaKalem k) {
        BigDecimal birim = k.getBirimFiyat() != null ? k.getBirimFiyat() : BigDecimal.ZERO;
        BigDecimal iskonto = k.getIskontoOrani() != null ? k.getIskontoOrani() : BigDecimal.ZERO;
        if (iskonto.signum() <= 0) return birim;
        return birim.multiply(BigDecimal.ONE.subtract(
                iskonto.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
    }

    private BigDecimal para(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(OLCEK, RoundingMode.HALF_UP);
    }

    private BigDecimal marj(BigDecimal kar, BigDecimal ciro) {
        if (ciro == null || ciro.signum() == 0) return BigDecimal.ZERO;
        return kar.multiply(BigDecimal.valueOf(100)).divide(ciro, OLCEK, RoundingMode.HALF_UP);
    }
}
