package com.raspel.erp.service.sistem;

import com.raspel.erp.dto.sistem.KarlilikAnalizDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.entity.ticaret.IadeKalem;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.IadeKalemRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
import com.raspel.erp.service.envanter.MaliyetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gelişmiş kârlılık analizi. Satış (KESILDI) fatura kalemlerinden ciro ve COGS
 * (ağırlıklı ortalama maliyet / satış anı anlık görüntüsü) hesaplar; SATIS iadelerini düşer.
 * Kırılım ürün / kategori / cari bazında yapılabilir.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class KarlilikService {

    private static final int OLCEK = 2;
    private static final int NEGATIF_LIMIT = 10;

    private final FaturaRepository faturaRepository;
    private final FaturaKalemRepository faturaKalemRepository;
    private final StokRepository stokRepository;
    private final IadeRepository iadeRepository;
    private final IadeKalemRepository iadeKalemRepository;
    private final MaliyetService maliyetService;

    public KarlilikAnalizDTO karlilikAnalizi(Long sirketId, LocalDate baslangic, LocalDate bitis, String grup) {
        String grp = normalizeGrup(grup);
        LocalDate bas = baslangic != null ? baslangic : LocalDate.now().withDayOfMonth(1);
        LocalDate bit = bitis != null ? bitis : LocalDate.now();

        Map<Long, Stok> stokCache = new java.util.HashMap<>();
        Map<String, BigDecimal[]> aylik = new java.util.TreeMap<>(); // ay -> [ciro, maliyet]
        Map<String, KirilimAcc> kirilim = new LinkedHashMap<>();

        BigDecimal[] toplam = {BigDecimal.ZERO, BigDecimal.ZERO};
        int[] kalemSayisi = {0};

        List<Fatura> faturalar = faturaRepository.findBySirketIdAndTarihBetweenKalemli(sirketId, bas, bit);
        for (Fatura f : faturalar) {
            if (f.getTur() != Fatura.FaturaTur.SATIS || f.getDurum() != Fatura.FaturaDurum.KESILDI) continue;
            String ay = f.getTarih() != null ? YearMonth.from(f.getTarih()).toString() : null;
            // kalemler fatura sorgusunda EntityGraph ile eager yüklenir; ek sorgu gerekmez.
            for (FaturaKalem k : f.getKalemler()) {
                BigDecimal adet = k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO;
                if (adet.signum() == 0) continue;
                BigDecimal netBirim = netBirim(k);
                BigDecimal ciro = netBirim.multiply(adet);
                BigDecimal maliyet = birimMaliyet(k, stokCache).multiply(adet).setScale(OLCEK, RoundingMode.HALF_UP);
                kirilim(kirilim, grp, f, k, stokCache, ciro, maliyet);
                if (ay != null) {
                    BigDecimal[] agg = aylik.computeIfAbsent(ay, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    agg[0] = agg[0].add(ciro);
                    agg[1] = agg[1].add(maliyet);
                }
                toplam[0] = toplam[0].add(ciro);
                toplam[1] = toplam[1].add(maliyet);
                kalemSayisi[0]++;
            }
        }

        // SATIS iadeleri düşülür (iade kalemleri tek sorguda toplu yüklenir).
        BigDecimal iadeCiro = BigDecimal.ZERO;
        BigDecimal iadeMaliyet = BigDecimal.ZERO;
        List<Iade> iadeler = iadeRepository.findBySirketIdAndTurAndDurumAndTarihBetween(
                sirketId, "SATIS", "TAMAMLANDI", bas, bit);
        Map<Long, List<IadeKalem>> iadeKalemMap = iadeler.isEmpty() ? Map.of()
                : iadeKalemRepository.findByIadeIdIn(
                        iadeler.stream().map(Iade::getId).collect(java.util.stream.Collectors.toList()))
                    .stream().collect(java.util.stream.Collectors.groupingBy(IadeKalem::getIadeId));
        for (Iade iade : iadeler) {
            String ay = iade.getTarih() != null ? YearMonth.from(iade.getTarih()).toString() : null;
            for (IadeKalem ik : iadeKalemMap.getOrDefault(iade.getId(), List.of())) {
                BigDecimal miktar = ik.getMiktar() != null ? ik.getMiktar() : BigDecimal.ZERO;
                if (miktar.signum() == 0) continue;
                BigDecimal ciro = (ik.getBirimFiyat() != null ? ik.getBirimFiyat() : BigDecimal.ZERO).multiply(miktar);
                Stok stok = ik.getStokId() != null ? stokGetir(ik.getStokId(), stokCache) : null;
                BigDecimal birimM = stok != null ? maliyetService.ortalamaMaliyet(stok) : BigDecimal.ZERO;
                if (birimM == null) birimM = BigDecimal.ZERO;
                BigDecimal maliyet = birimM.multiply(miktar).setScale(OLCEK, RoundingMode.HALF_UP);
                Fatura kaynakFatura = iade.getFaturaId() != null
                        ? faturaRepository.findById(iade.getFaturaId()).orElse(null) : null;
                iadeKirilim(kirilim, grp, iade, ik, stok, kaynakFatura, ciro, maliyet);
                if (ay != null) {
                    BigDecimal[] agg = aylik.computeIfAbsent(ay, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    agg[0] = agg[0].subtract(ciro);
                    agg[1] = agg[1].subtract(maliyet);
                }
                toplam[0] = toplam[0].subtract(ciro);
                toplam[1] = toplam[1].subtract(maliyet);
                iadeCiro = iadeCiro.add(ciro);
                iadeMaliyet = iadeMaliyet.add(maliyet);
            }
        }

        List<KarlilikAnalizDTO.AylikKarlilik> trend = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : aylik.entrySet()) {
            BigDecimal ciro = para(e.getValue()[0]);
            BigDecimal maliyet = para(e.getValue()[1]);
            BigDecimal kar = ciro.subtract(maliyet);
            trend.add(KarlilikAnalizDTO.AylikKarlilik.builder()
                    .ay(e.getKey()).ciro(ciro).maliyet(maliyet).brutKar(kar).marj(marj(kar, ciro)).build());
        }

        BigDecimal toplamCiro = para(toplam[0]);
        List<KarlilikAnalizDTO.Kirilim> liste = new ArrayList<>();
        for (KirilimAcc a : kirilim.values()) {
            BigDecimal ciro = para(a.ciro);
            BigDecimal maliyet = para(a.maliyet);
            BigDecimal kar = ciro.subtract(maliyet);
            liste.add(KarlilikAnalizDTO.Kirilim.builder()
                    .id(a.id).ad(a.ad).ciro(ciro).maliyet(maliyet).brutKar(kar)
                    .marj(marj(kar, ciro)).pay(pay(ciro, toplamCiro)).build());
        }
        liste.sort(Comparator.comparing(KarlilikAnalizDTO.Kirilim::getBrutKar).reversed());

        List<KarlilikAnalizDTO.Kirilim> negatif = liste.stream()
                .filter(k -> k.getBrutKar().signum() < 0)
                .sorted(Comparator.comparing(KarlilikAnalizDTO.Kirilim::getBrutKar))
                .limit(NEGATIF_LIMIT)
                .toList();

        BigDecimal brutKar = toplamCiro.subtract(para(toplam[1]));
        return KarlilikAnalizDTO.builder()
                .grup(grp)
                .ozet(KarlilikAnalizDTO.Ozet.builder()
                        .ciro(toplamCiro).maliyet(para(toplam[1])).brutKar(brutKar)
                        .brutKarMarji(marj(brutKar, toplamCiro))
                        .iadeTutari(para(iadeCiro)).iadeMaliyeti(para(iadeMaliyet))
                        .kalemSayisi(kalemSayisi[0])
                        .negatifMarjliAdet((int) liste.stream().filter(k -> k.getBrutKar().signum() < 0).count())
                        .build())
                .aylikTrend(trend)
                .kirilim(liste)
                .negatifMarjli(negatif)
                .build();
    }

    // ---------- yardımcılar ----------

    private static final class KirilimAcc {
        Long id;
        String ad;
        BigDecimal ciro = BigDecimal.ZERO;
        BigDecimal maliyet = BigDecimal.ZERO;
    }

    private void kirilim(Map<String, KirilimAcc> map, String grp, Fatura f, FaturaKalem k,
                         Map<Long, Stok> cache, BigDecimal ciro, BigDecimal maliyet) {
        String[] anahtar = anahtar(grp, f, k, cache);
        KirilimAcc acc = map.computeIfAbsent(anahtar[0], x -> {
            KirilimAcc a = new KirilimAcc();
            a.ad = anahtar[1];
            a.id = anahtar[2] != null ? Long.valueOf(anahtar[2]) : null;
            return a;
        });
        acc.ciro = acc.ciro.add(ciro);
        acc.maliyet = acc.maliyet.add(maliyet);
    }

    private void iadeKirilim(Map<String, KirilimAcc> map, String grp, Iade iade, IadeKalem ik,
                             Stok stok, Fatura kaynakFatura, BigDecimal ciro, BigDecimal maliyet) {
        // Anahtar, satış kırılımıyla aynı olmalı: URUN -> stokId, CARI -> kaynak faturanın cariId'si.
        String key;
        switch (grp) {
            case "URUN" -> key = "U:" + ik.getStokId();
            case "CARI" -> key = "C:" + (kaynakFatura != null && kaynakFatura.getCariHesap() != null
                    ? kaynakFatura.getCariHesap().getId() : "genel");
            default -> {
                String kat = stok != null && stok.getKategori() != null && !stok.getKategori().isBlank()
                        ? stok.getKategori() : "Kategorisiz";
                key = "K:" + kat;
            }
        }
        KirilimAcc acc = map.get(key);
        if (acc == null) return;
        acc.ciro = acc.ciro.subtract(ciro);
        acc.maliyet = acc.maliyet.subtract(maliyet);
    }


    private String[] anahtar(String grp, Fatura f, FaturaKalem k, Map<Long, Stok> cache) {
        Stok stok = k.getStokId() != null ? stokGetir(k.getStokId(), cache) : null;
        return switch (grp) {
            case "URUN" -> new String[]{"U:" + k.getStokId(),
                    stok != null ? stok.getAd() : "Bilinmeyen ürün", k.getStokId() != null ? k.getStokId().toString() : null};
            case "CARI" -> new String[]{"C:" + (f.getCariHesap() != null ? f.getCariHesap().getId() : "genel"),
                    f.getCariHesap() != null ? f.getCariHesap().getAd() : "Genel",
                    f.getCariHesap() != null ? f.getCariHesap().getId().toString() : null};
            default -> {
                String kat = stok != null && stok.getKategori() != null && !stok.getKategori().isBlank()
                        ? stok.getKategori() : "Kategorisiz";
                yield new String[]{"K:" + kat, kat, null};
            }
        };
    }

    private Stok stokGetir(Long stokId, Map<Long, Stok> cache) {
        return cache.computeIfAbsent(stokId, id -> stokRepository.findById(id).orElse(null));
    }

    private BigDecimal netBirim(FaturaKalem k) {
        BigDecimal birim = k.getBirimFiyat() != null ? k.getBirimFiyat() : BigDecimal.ZERO;
        BigDecimal iskonto = k.getIskontoOrani() != null ? k.getIskontoOrani() : BigDecimal.ZERO;
        if (iskonto.signum() <= 0) return birim;
        return birim.multiply(BigDecimal.ONE.subtract(iskonto.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
    }

    private BigDecimal birimMaliyet(FaturaKalem k, Map<Long, Stok> cache) {
        if (k.getBirimMaliyet() != null && k.getBirimMaliyet().signum() > 0) return k.getBirimMaliyet();
        Stok stok = k.getStokId() != null ? stokGetir(k.getStokId(), cache) : null;
        return stok != null ? maliyetService.ortalamaMaliyet(stok) : BigDecimal.ZERO;
    }

    private String normalizeGrup(String grup) {
        if (grup == null) return "KATEGORI";
        String g = grup.trim().toUpperCase();
        return switch (g) {
            case "URUN", "CARI", "KATEGORI" -> g;
            default -> "KATEGORI";
        };
    }

    private BigDecimal para(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(OLCEK, RoundingMode.HALF_UP);
    }

    private BigDecimal marj(BigDecimal kar, BigDecimal ciro) {
        if (ciro == null || ciro.signum() == 0) return BigDecimal.ZERO;
        return kar.multiply(BigDecimal.valueOf(100)).divide(ciro, OLCEK, RoundingMode.HALF_UP);
    }

    private BigDecimal pay(BigDecimal ciro, BigDecimal toplam) {
        if (toplam == null || toplam.signum() == 0) return BigDecimal.ZERO;
        return ciro.multiply(BigDecimal.valueOf(100)).divide(toplam, OLCEK, RoundingMode.HALF_UP);
    }
}
