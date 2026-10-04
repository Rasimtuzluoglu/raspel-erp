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

    /**
     * Iade verisi icin toplu on yukleme: iade kalemleri ve kaynak faturalar
     * (cari hesaplariyla) sabit sayida sorguda cozulur.
     *
     * <p>Not: iade kaynak fatura id'si null olabilir. {@link Map#of()} ile uretilen
     * degismez haritalar {@code get(null)} cagrisinda NPE firlattigi icin bos
     * durumda {@link java.util.HashMap} kullanilir ve erisim {@link #fatura(Long)}
     * uzerinden null-guvenli yapilir.
     */
    private record IadeToplu(
            Map<Long, List<IadeKalem>> kalemler,
            Map<Long, Fatura> kaynakFaturalar) {
        static IadeToplu bos() {
            return new IadeToplu(new java.util.HashMap<>(), new java.util.HashMap<>());
        }

        List<IadeKalem> kalem(Long iadeId) {
            if (iadeId == null) return List.of();
            List<IadeKalem> liste = kalemler.get(iadeId);
            return liste != null ? liste : List.of();
        }

        Fatura fatura(Long faturaId) {
            return faturaId == null ? null : kaynakFaturalar.get(faturaId);
        }
    }

    /**
     * N+1 onlemi: iade basina findByIadeId + findById cagrilmasini engeller.
     * Kaynak fatura ve cari hesap tek sorguda join ile gelir; kalemler toplu alinir.
     */
    private IadeToplu iadeTopluYukle(List<Iade> iadeler) {
        if (iadeler.isEmpty()) return IadeToplu.bos();
        List<Long> iadeIdleri = iadeler.stream().map(Iade::getId)
                .filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
        Map<Long, List<IadeKalem>> kalemler = iadeIdleri.isEmpty() ? new java.util.HashMap<>()
                : iadeKalemRepository.findByIadeIdIn(iadeIdleri).stream()
                        .collect(java.util.stream.Collectors.groupingBy(IadeKalem::getIadeId));
        List<Long> faturaIdleri = iadeler.stream().map(Iade::getFaturaId)
                .filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
        Map<Long, Fatura> faturalar = faturaIdleri.isEmpty() ? new java.util.HashMap<>()
                : faturaRepository.findAllByIdIn(faturaIdleri).stream()
                        .collect(java.util.stream.Collectors.toMap(Fatura::getId, f -> f, (a, b) -> a));
        return new IadeToplu(kalemler, faturalar);
    }

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

        // SATIS iadeleri düşülür (iade kalemleri ve kaynak faturalar tek sorguda toplu yüklenir).
        BigDecimal iadeCiro = BigDecimal.ZERO;
        BigDecimal iadeMaliyet = BigDecimal.ZERO;
        List<Iade> iadeler = iadeRepository.findBySirketIdAndTurAndDurumAndTarihBetween(
                sirketId, "SATIS", "TAMAMLANDI", bas, bit);
        IadeToplu iadeToplu = iadeTopluYukle(iadeler);
        for (Iade iade : iadeler) {
            String ay = iade.getTarih() != null ? YearMonth.from(iade.getTarih()).toString() : null;
            Fatura kaynakFatura = iadeToplu.fatura(iade.getFaturaId());
            for (IadeKalem ik : iadeToplu.kalem(iade.getId())) {
                BigDecimal miktar = ik.getMiktar() != null ? ik.getMiktar() : BigDecimal.ZERO;
                if (miktar.signum() == 0) continue;
                BigDecimal ciro = (ik.getBirimFiyat() != null ? ik.getBirimFiyat() : BigDecimal.ZERO).multiply(miktar);
                Stok stok = ik.getStokId() != null ? stokGetir(ik.getStokId(), stokCache) : null;
                BigDecimal birimM = stok != null ? maliyetService.ortalamaMaliyet(stok) : BigDecimal.ZERO;
                if (birimM == null) birimM = BigDecimal.ZERO;
                BigDecimal maliyet = birimM.multiply(miktar).setScale(OLCEK, RoundingMode.HALF_UP);
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

    /**
     * Drill-down: seçilen kırılım değeri (kategori adı / ürün / cari) için alt kırılım
     * ve belge bazlı (fatura kalemi) kâr dökümü üretir. Satış iadeleri negatif satır
     * olarak düşülür; böylece detay toplamı ana ekranla tutarlı kalır.
     */
    public com.raspel.erp.dto.sistem.KarlilikDetayDTO karlilikDetay(Long sirketId, LocalDate baslangic,
                                                                    LocalDate bitis, String grup,
                                                                    String deger, Long degerId) {
        String grp = normalizeGrup(grup);
        String altGrup = switch (grp) {
            case "KATEGORI" -> "URUN";
            case "URUN" -> "CARI";
            default -> "URUN";
        };
        LocalDate bas = baslangic != null ? baslangic : LocalDate.now().withDayOfMonth(1);
        LocalDate bit = bitis != null ? bitis : LocalDate.now();

        Map<Long, Stok> stokCache = new java.util.HashMap<>();
        Map<String, KirilimAcc> altKirilim = new LinkedHashMap<>();
        List<com.raspel.erp.dto.sistem.KarlilikDetayDTO.Belge> belgeler = new ArrayList<>();
        BigDecimal[] toplam = {BigDecimal.ZERO, BigDecimal.ZERO};
        String[] hedefAd = {deger};

        List<Fatura> faturalar = faturaRepository.findBySirketIdAndTarihBetweenKalemli(sirketId, bas, bit);
        for (Fatura f : faturalar) {
            if (f.getTur() != Fatura.FaturaTur.SATIS || f.getDurum() != Fatura.FaturaDurum.KESILDI) continue;
            for (FaturaKalem k : f.getKalemler()) {
                BigDecimal adet = k.getAdet() != null ? k.getAdet() : BigDecimal.ZERO;
                if (adet.signum() == 0) continue;
                Stok stok = k.getStokId() != null ? stokGetir(k.getStokId(), stokCache) : null;
                if (!eslesiyorMu(grp, f, stok, k.getStokId(), degerId, deger)) continue;
                if (hedefAd[0] == null) hedefAd[0] = gorunenAd(grp, f, stok);

                BigDecimal ciro = netBirim(k).multiply(adet);
                BigDecimal maliyet = birimMaliyet(k, stokCache).multiply(adet).setScale(OLCEK, RoundingMode.HALF_UP);
                String[] alt = altAnahtar(altGrup, f, k.getStokId(), stok);
                KirilimAcc acc = altKirilim.computeIfAbsent(alt[0], x -> {
                    KirilimAcc a = new KirilimAcc();
                    a.ad = alt[1];
                    a.id = alt[2] != null ? Long.valueOf(alt[2]) : null;
                    return a;
                });
                acc.ciro = acc.ciro.add(ciro);
                acc.maliyet = acc.maliyet.add(maliyet);
                toplam[0] = toplam[0].add(ciro);
                toplam[1] = toplam[1].add(maliyet);

                belgeler.add(com.raspel.erp.dto.sistem.KarlilikDetayDTO.Belge.builder()
                        .faturaId(f.getId()).faturaNumarasi(f.getFaturaNumarasi()).tarih(f.getTarih())
                        .cariAd(f.getCariHesap() != null ? f.getCariHesap().getAd() : "Genel")
                        .urunAd(stok != null ? stok.getAd() : "Bilinmeyen ürün")
                        .adet(adet).ciro(ciro).maliyet(maliyet)
                        .brutKar(ciro.subtract(maliyet)).iade(false)
                        .build());
            }
        }

        // İadeler negatif satır olarak düşülür (kalemler ve kaynak faturalar toplu yüklenir).
        List<Iade> iadeler = iadeRepository.findBySirketIdAndTurAndDurumAndTarihBetween(
                sirketId, "SATIS", "TAMAMLANDI", bas, bit);
        IadeToplu iadeToplu = iadeTopluYukle(iadeler);
        for (Iade iade : iadeler) {
            Fatura kaynakFatura = iadeToplu.fatura(iade.getFaturaId());
            for (IadeKalem ik : iadeToplu.kalem(iade.getId())) {
                BigDecimal miktar = ik.getMiktar() != null ? ik.getMiktar() : BigDecimal.ZERO;
                if (miktar.signum() == 0) continue;
                Stok stok = ik.getStokId() != null ? stokGetir(ik.getStokId(), stokCache) : null;
                if (!eslesiyorMu(grp, kaynakFatura, stok, ik.getStokId(), degerId, deger)) continue;
                if (hedefAd[0] == null) hedefAd[0] = gorunenAd(grp, kaynakFatura, stok);

                BigDecimal ciro = (ik.getBirimFiyat() != null ? ik.getBirimFiyat() : BigDecimal.ZERO).multiply(miktar);
                BigDecimal birimM = stok != null ? maliyetService.ortalamaMaliyet(stok) : BigDecimal.ZERO;
                if (birimM == null) birimM = BigDecimal.ZERO;
                BigDecimal maliyet = birimM.multiply(miktar).setScale(OLCEK, RoundingMode.HALF_UP);
                String[] alt = altAnahtar(altGrup, kaynakFatura, ik.getStokId(), stok);
                KirilimAcc acc = altKirilim.computeIfAbsent(alt[0], x -> {
                    KirilimAcc a = new KirilimAcc();
                    a.ad = alt[1];
                    a.id = alt[2] != null ? Long.valueOf(alt[2]) : null;
                    return a;
                });
                acc.ciro = acc.ciro.subtract(ciro);
                acc.maliyet = acc.maliyet.subtract(maliyet);
                toplam[0] = toplam[0].subtract(ciro);
                toplam[1] = toplam[1].subtract(maliyet);

                belgeler.add(com.raspel.erp.dto.sistem.KarlilikDetayDTO.Belge.builder()
                        .faturaId(iade.getFaturaId()).faturaNumarasi("İade #" + iade.getId()).tarih(iade.getTarih())
                        .cariAd(kaynakFatura != null && kaynakFatura.getCariHesap() != null
                                ? kaynakFatura.getCariHesap().getAd() : "Genel")
                        .urunAd(stok != null ? stok.getAd() : "Bilinmeyen ürün")
                        .adet(miktar).ciro(ciro).maliyet(maliyet)
                        .brutKar(ciro.subtract(maliyet)).iade(true)
                        .build());
            }
        }

        BigDecimal toplamCiro = para(toplam[0]);
        BigDecimal brutKar = toplamCiro.subtract(para(toplam[1]));
        List<KarlilikAnalizDTO.Kirilim> altListe = new ArrayList<>();
        for (KirilimAcc a : altKirilim.values()) {
            BigDecimal ciro = para(a.ciro);
            BigDecimal maliyet = para(a.maliyet);
            BigDecimal kar = ciro.subtract(maliyet);
            altListe.add(KarlilikAnalizDTO.Kirilim.builder()
                    .id(a.id).ad(a.ad).ciro(ciro).maliyet(maliyet).brutKar(kar)
                    .marj(marj(kar, ciro)).pay(pay(ciro, toplamCiro)).build());
        }
        altListe.sort(Comparator.comparing(KarlilikAnalizDTO.Kirilim::getBrutKar).reversed());
        belgeler.sort(Comparator.comparing(com.raspel.erp.dto.sistem.KarlilikDetayDTO.Belge::getTarih,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return com.raspel.erp.dto.sistem.KarlilikDetayDTO.builder()
                .grup(grp).deger(hedefAd[0]).altGrup(altGrup)
                .ciro(toplamCiro).maliyet(para(toplam[1])).brutKar(brutKar)
                .marj(marj(brutKar, toplamCiro))
                .altKirilim(altListe)
                .belgeler(belgeler)
                .build();
    }

    /** Satır seçilen kırılım değeriyle eşleşiyor mu? (id veya ad eşleşmesi) */
    private boolean eslesiyorMu(String grp, Fatura f, Stok stok, Long stokId, Long degerId, String deger) {
        switch (grp) {
            case "URUN":
                if (degerId != null && stokId != null && degerId.equals(stokId)) return true;
                return deger != null && stok != null && deger.equalsIgnoreCase(stok.getAd());
            case "CARI":
                Long cariId = f != null && f.getCariHesap() != null ? f.getCariHesap().getId() : null;
                if (degerId != null && cariId != null && degerId.equals(cariId)) return true;
                String cariAd = f != null && f.getCariHesap() != null ? f.getCariHesap().getAd() : null;
                return deger != null && cariAd != null && deger.equalsIgnoreCase(cariAd);
            default:
                String kat = stok != null && stok.getKategori() != null && !stok.getKategori().isBlank()
                        ? stok.getKategori() : "Kategorisiz";
                return deger != null && deger.equalsIgnoreCase(kat);
        }
    }

    private String gorunenAd(String grp, Fatura f, Stok stok) {
        return switch (grp) {
            case "URUN" -> stok != null ? stok.getAd() : "Bilinmeyen ürün";
            case "CARI" -> f != null && f.getCariHesap() != null ? f.getCariHesap().getAd() : "Genel";
            default -> stok != null && stok.getKategori() != null && !stok.getKategori().isBlank()
                    ? stok.getKategori() : "Kategorisiz";
        };
    }

    private String[] altAnahtar(String altGrup, Fatura f, Long stokId, Stok stok) {
        return switch (altGrup) {
            case "CARI" -> new String[]{"C:" + (f != null && f.getCariHesap() != null ? f.getCariHesap().getId() : "genel"),
                    f != null && f.getCariHesap() != null ? f.getCariHesap().getAd() : "Genel",
                    f != null && f.getCariHesap() != null ? f.getCariHesap().getId().toString() : null};
            case "URUN" -> new String[]{"U:" + stokId,
                    stok != null ? stok.getAd() : "Bilinmeyen ürün",
                    stokId != null ? stokId.toString() : null};
            default -> {
                String kat = stok != null && stok.getKategori() != null && !stok.getKategori().isBlank()
                        ? stok.getKategori() : "Kategorisiz";
                yield new String[]{"K:" + kat, kat, null};
            }
        };
    }


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
        Long accId;
        String ad;
        switch (grp) {
            case "URUN" -> {
                key = "U:" + ik.getStokId();
                accId = ik.getStokId();
                ad = stok != null ? stok.getAd() : "Bilinmeyen ürün";
            }
            case "CARI" -> {
                Long cariId = kaynakFatura != null && kaynakFatura.getCariHesap() != null
                        ? kaynakFatura.getCariHesap().getId() : null;
                key = "C:" + (cariId != null ? cariId : "genel");
                accId = cariId;
                ad = kaynakFatura != null && kaynakFatura.getCariHesap() != null
                        ? kaynakFatura.getCariHesap().getAd() : "Genel";
            }
            default -> {
                String kat = stok != null && stok.getKategori() != null && !stok.getKategori().isBlank()
                        ? stok.getKategori() : "Kategorisiz";
                key = "K:" + kat;
                accId = null;
                ad = kat;
            }
        }
        KirilimAcc acc = map.get(key);
        if (acc == null) {
            // Dönem içinde eşleşen ileri satış yoksa da iade kırılımda negatif satır olarak görünmeli;
            // aksi halde kırılım toplamı ile özet toplam tutarsız kalır.
            acc = new KirilimAcc();
            acc.id = accId;
            acc.ad = ad;
            acc.ciro = BigDecimal.ZERO;
            acc.maliyet = BigDecimal.ZERO;
            map.put(key, acc);
        }
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

    /**
 * Satırın KDV HARIÇ net tutarı.
 *
 * <p><b>DÜZELTME:</b> Önceden {@code netBirim} yalnızca satır iskonto oranını
 * uyguluyordu; KDV'yi ayrıştırmıyordu. Karlılık raporunda "net" olarak
 * gösterilen tutar aslında KDV DAHİL brüt tutardı. Bu, üç ayrı para hesabı
 * yaratıyordu:
 * <ol>
 *   <li>{@code FaturaTutar} (kayıt/fiş tarafı, doğru),</li>
 *   <li>{@code KarlilikService.netBirim} (rapor tarafı, KDV dahil — YANLIŞ),</li>
 *   <li>frontend {@code faturaHesapla.js} (yuvarlamasız — tutarsız).</li>
 * </ol>
 * Artık {@link com.raspel.erp.util.FaturaTutar#satir} kullanılır: aynı
 * formül, aynı yuvarlama, tek kaynak.
 */
private BigDecimal netBirim(FaturaKalem k) {
        return com.raspel.erp.util.FaturaTutar.satir(
                k.getBirimFiyat(),
                BigDecimal.ONE,
                k.getIskontoOrani(),
                k.getKdvOrani()).net();
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
