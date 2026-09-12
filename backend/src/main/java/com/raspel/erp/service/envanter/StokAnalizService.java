package com.raspel.erp.service.envanter;

import com.raspel.erp.dto.envanter.AlisOzetDTO;
import com.raspel.erp.dto.envanter.AylikFiyatDTO;
import com.raspel.erp.dto.envanter.IslemGecmisiSayfaliDTO;
import com.raspel.erp.dto.envanter.IslemSatirDTO;
import com.raspel.erp.dto.envanter.KarlilikDTO;
import com.raspel.erp.dto.envanter.MusteriAnalizDTO;
import com.raspel.erp.dto.envanter.SatisOzetDTO;
import com.raspel.erp.dto.envanter.StokAnalizDTO;
import com.raspel.erp.dto.envanter.TedarikciAnalizDTO;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.ticaret.FaturaAnalizSatirProjeksiyon;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.IadeAnalizSatirProjeksiyon;
import com.raspel.erp.repository.ticaret.IadeKalemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Ürün maliyet / alış-satış / kârlılık analizleri.
 *
 * Veri kaynağı gerçek hareketlerdir: kesilmiş faturalar (ALIS/SATIS + KESILDI)
 * ve tamamlanmış iadeler (TAMAMLANDI). İptal edilmiş kayıtlar analize dahil
 * edilmez. Mekanik stok logları (stok_hareket) fiyat bilgisi taşımadığı için
 * fiyat analizlerinde kullanılmaz.
 *
 * Ortalamalar tartılı hesaplanır: Σ(miktar×netBirimFiyat) / Σ(miktar).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StokAnalizService {

    private final StokRepository stokRepository;
    private final FaturaKalemRepository faturaKalemRepository;
    private final IadeKalemRepository iadeKalemRepository;

    private static final String IADE_ALIS = "ALIS";
    private static final String IADE_SATIS = "SATIS";
    private static final String IADE_DURUM = "TAMAMLANDI";
    private static final int PARA_OLCEGI = 2;
    private static final int VARSAYILAN_LIMIT = 500;
    private static final int EN_YUKSEK_LIMIT = 2000;
    private static final int CAri_LISTE_LIMITI = 500;
    private static final int AY_LISTE_LIMITI = 600;
    private static final int SAYFA_BOYUT_VARSAYILAN = 20;
    private static final int SAYFA_BOYUT_MAX = 200;

    private record SatirOlcum(BigDecimal miktar, BigDecimal netBirimFiyat, BigDecimal netTutar,
                              LocalDate tarih, String cariAd) {

        SatirOlcum(BigDecimal miktar, BigDecimal netBirimFiyat, BigDecimal netTutar, LocalDate tarih) {
            this(miktar, netBirimFiyat, netTutar, tarih, null);
        }
    }

    private record HareketToplami(BigDecimal miktar, BigDecimal tutar, BigDecimal ortalamaFiyat,
                                  BigDecimal sonFiyat, LocalDate sonTarih,
                                  BigDecimal enDusukFiyat, BigDecimal enYuksekFiyat, int islemSayisi) {
    }

    // ---------- Tek bordo / yetki ----------

    public StokAnalizDTO analiz(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis) {
        tarihDogruMu(baslangic, bitis);
        Stok stok = bulYetkiliStok(stokId, sirketId);
        return StokAnalizDTO.builder()
                .stokId(stok.getId())
                .stokAd(stok.getAd())
                .stokKodu(stok.getStokKodu())
                .birim(stok.getBirim())
                .alisOzet(alisOzet(sirketId, stok, baslangic, bitis))
                .satisOzet(satisOzet(sirketId, stok, baslangic, bitis))
                .karlilik(karlilik(sirketId, stok, baslangic, bitis))
                .build();
    }

    public AlisOzetDTO alisOzet(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis) {
        tarihDogruMu(baslangic, bitis);
        Stok stok = bulYetkiliStok(stokId, sirketId);
        return alisOzet(sirketId, stok, baslangic, bitis);
    }

    public SatisOzetDTO satisOzet(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis) {
        tarihDogruMu(baslangic, bitis);
        Stok stok = bulYetkiliStok(stokId, sirketId);
        return satisOzet(sirketId, stok, baslangic, bitis);
    }

    public KarlilikDTO karlilik(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis) {
        tarihDogruMu(baslangic, bitis);
        Stok stok = bulYetkiliStok(stokId, sirketId);
        return karlilik(sirketId, stok, baslangic, bitis);
    }

    public List<TedarikciAnalizDTO> tedarikciAnaliz(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis) {
        tarihDogruMu(baslangic, bitis);
        bulYetkiliStok(stokId, sirketId);
        return cariAnaliz(sirketId, stokId, Fatura.FaturaTur.ALIS, baslangic, bitis).stream()
                .map(g -> TedarikciAnalizDTO.builder()
                        .cariHesapId(g.cariHesapId())
                        .cariHesapAd(g.cariHesapAd())
                        .toplamMiktar(g.toplamMiktar())
                        .toplamTutar(g.toplamTutar())
                        .ortalamaFiyat(g.ortalamaFiyat())
                        .sonFiyat(g.sonFiyat())
                        .sonAlisTarihi(g.sonTarih())
                        .islemSayisi(g.islemSayisi())
                        .build())
                .collect(Collectors.toList());
    }

    public List<MusteriAnalizDTO> musteriAnaliz(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis) {
        tarihDogruMu(baslangic, bitis);
        bulYetkiliStok(stokId, sirketId);
        return cariAnaliz(sirketId, stokId, Fatura.FaturaTur.SATIS, baslangic, bitis).stream()
                .map(g -> MusteriAnalizDTO.builder()
                        .cariHesapId(g.cariHesapId())
                        .cariHesapAd(g.cariHesapAd())
                        .toplamMiktar(g.toplamMiktar())
                        .toplamTutar(g.toplamTutar())
                        .ortalamaFiyat(g.ortalamaFiyat())
                        .sonFiyat(g.sonFiyat())
                        .sonSatisTarihi(g.sonTarih())
                        .islemSayisi(g.islemSayisi())
                        .build())
                .collect(Collectors.toList());
    }

    public List<IslemSatirDTO> islemGecmisi(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis, boolean artan) {
        return islemGecmisi(sirketId, stokId, baslangic, bitis, artan, null);
    }

    public List<IslemSatirDTO> islemGecmisi(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis,
                                            boolean artan, Integer limit) {
        tarihDogruMu(baslangic, bitis);
        bulYetkiliStok(stokId, sirketId);
        List<IslemSatirDTO> satirlar = islemSatirlari(sirketId, stokId, baslangic, bitis, artan);
        int ustSinir = (limit == null || limit < 1) ? VARSAYILAN_LIMIT : Math.min(limit, EN_YUKSEK_LIMIT);
        return sinirla(satirlar, ustSinir);
    }

    public IslemGecmisiSayfaliDTO islemGecmisiSayfali(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis,
                                                      boolean artan, Integer sayfaParam, Integer boyutParam) {
        tarihDogruMu(baslangic, bitis);
        bulYetkiliStok(stokId, sirketId);
        List<IslemSatirDTO> satirlar = islemSatirlari(sirketId, stokId, baslangic, bitis, artan);
        int boyut = (boyutParam == null || boyutParam < 1) ? SAYFA_BOYUT_VARSAYILAN : Math.min(boyutParam, SAYFA_BOYUT_MAX);
        int sayfa = (sayfaParam == null || sayfaParam < 0) ? 0 : sayfaParam;
        int toplam = satirlar.size();
        int toplamSayfa = toplam == 0 ? 0 : (int) Math.ceil((double) toplam / boyut);
        int bas = sayfa * boyut;
        List<IslemSatirDTO> dilim = bas >= toplam
                ? List.of()
                : new ArrayList<>(satirlar.subList(bas, Math.min(bas + boyut, toplam)));
        return IslemGecmisiSayfaliDTO.builder()
                .satirlar(dilim)
                .toplam(toplam)
                .sayfa(sayfa)
                .boyut(boyut)
                .toplamSayfa(toplamSayfa)
                .build();
    }

    /** İşlem geçmişi satırlarını tüm kaynaklardan toplar ve istenen yönde sıralar (sayfalama öncesi ham liste). */
    private List<IslemSatirDTO> islemSatirlari(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis,
                                               boolean artan) {
        List<IslemSatirDTO> satirlar = new ArrayList<>();
        for (FaturaAnalizSatirProjeksiyon s : faturaKalemRepository.analizSatirlari(
                stokId, sirketId, Fatura.FaturaTur.ALIS, Fatura.FaturaDurum.KESILDI, baslangic, bitis)) {
            satirlar.add(IslemSatirDTO.builder()
                    .tarih(s.getFaturaTarihi()).belgeNo(s.getFaturaNumarasi()).tur("ALIS")
                    .cariHesapAd(s.getCariHesapAd()).miktar(BigDecimal.valueOf(s.getAdet()))
                    .birimFiyat(netFiyat(s.getBirimFiyat(), s.getIskontoOrani()))
                    .tutar(netTutar(s.getBirimFiyat(), s.getIskontoOrani(), BigDecimal.valueOf(s.getAdet())))
                    .build());
        }
        for (FaturaAnalizSatirProjeksiyon s : faturaKalemRepository.analizSatirlari(
                stokId, sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, baslangic, bitis)) {
            satirlar.add(IslemSatirDTO.builder()
                    .tarih(s.getFaturaTarihi()).belgeNo(s.getFaturaNumarasi()).tur("SATIS")
                    .cariHesapAd(s.getCariHesapAd()).miktar(BigDecimal.valueOf(s.getAdet()))
                    .birimFiyat(netFiyat(s.getBirimFiyat(), s.getIskontoOrani()))
                    .tutar(netTutar(s.getBirimFiyat(), s.getIskontoOrani(), BigDecimal.valueOf(s.getAdet())))
                    .build());
        }
        for (IadeAnalizSatirProjeksiyon i : iadeKalemRepository.analizSatirlari(
                stokId, sirketId, IADE_ALIS, IADE_DURUM, baslangic, bitis)) {
            satirlar.add(IslemSatirDTO.builder()
                    .tarih(i.getIadeTarihi()).belgeNo("İade #" + i.getIadeId()).tur("ALIS_IADE")
                    .cariHesapAd(i.getCariHesapAd()).miktar(i.getMiktar())
                    .birimFiyat(i.getBirimFiyat())
                    .tutar(iNetTutar(i))
                    .build());
        }
        for (IadeAnalizSatirProjeksiyon i : iadeKalemRepository.analizSatirlari(
                stokId, sirketId, IADE_SATIS, IADE_DURUM, baslangic, bitis)) {
            satirlar.add(IslemSatirDTO.builder()
                    .tarih(i.getIadeTarihi()).belgeNo("İade #" + i.getIadeId()).tur("SATIS_IADE")
                    .cariHesapAd(i.getCariHesapAd()).miktar(i.getMiktar())
                    .birimFiyat(i.getBirimFiyat())
                    .tutar(iNetTutar(i))
                    .build());
        }
        Comparator<IslemSatirDTO> sirala = Comparator.comparing(IslemSatirDTO::getTarih);
        if (!artan) sirala = sirala.reversed();
        satirlar.sort(sirala);
        return satirlar;
    }

    public List<AylikFiyatDTO> aylikFiyatGecmisi(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis) {
        tarihDogruMu(baslangic, bitis);
        bulYetkiliStok(stokId, sirketId);
        Map<YearMonth, AyToplami> aylar = new TreeMap<>();
        for (SatirOlcum o : alisOlcumleri(sirketId, stokId, baslangic, bitis)) {
            aylar.computeIfAbsent(YearMonth.from(o.tarih()), k -> new AyToplami())
                    .alisEkle(o.miktar(), o.netTutar());
        }
        for (SatirOlcum o : satisOlcumleri(sirketId, stokId, baslangic, bitis)) {
            aylar.computeIfAbsent(YearMonth.from(o.tarih()), k -> new AyToplami())
                    .satisEkle(o.miktar(), o.netTutar());
        }
        List<AylikFiyatDTO> sonuc = new ArrayList<>();
        for (Map.Entry<YearMonth, AyToplami> e : aylar.entrySet()) {
            sonuc.add(AylikFiyatDTO.builder()
                    .yil(e.getKey().getYear())
                    .ay(e.getKey().getMonthValue())
                    .ortalamaAlisFiyati(e.getValue().alisOrtalama())
                    .ortalamaSatisFiyati(e.getValue().satisOrtalama())
                    .toplamAlisMiktar(e.getValue().alisMiktar)
                    .toplamSatisMiktar(e.getValue().satisMiktar)
                    .build());
        }
        return sinirla(sonuc, AY_LISTE_LIMITI);
    }

    // ---------- İç/temel yöntemler ----------

    private void tarihDogruMu(LocalDate baslangic, LocalDate bitis) {
        if (baslangic != null && bitis != null && baslangic.isAfter(bitis)) {
            throw new BusinessException("Başlangıç tarihi bitiş tarihinden sonra olamaz.");
        }
    }

    private <T> List<T> sinirla(List<T> liste, int ustSinir) {
        return liste.size() <= ustSinir ? liste : new ArrayList<>(liste.subList(0, ustSinir));
    }

    private Stok bulYetkiliStok(Long stokId, Long sirketId) {
        Stok stok = stokRepository.findById(stokId)
                .orElseThrow(() -> new ResourceNotFoundException("Stok", stokId));
        if (!Objects.equals(stok.getSirketId(), sirketId)) {
            throw new BusinessException("Bu stoğa erişim yetkiniz bulunmuyor.");
        }
        return stok;
    }

    private AlisOzetDTO alisOzet(Long sirketId, Stok stok, LocalDate baslangic, LocalDate bitis) {
        HareketToplami t = toplam(alisOlcumleri(sirketId, stok.getId(), baslangic, bitis));
        return AlisOzetDTO.builder()
                .stokId(stok.getId()).stokAd(stok.getAd()).stokKodu(stok.getStokKodu())
                .stokMiktar(stok.getMiktar())
                .toplamAlisMiktar(t.miktar())
                .toplamAlisTutari(t.tutar())
                .ortalamaBirimFiyat(t.ortalamaFiyat())
                .sonAlisFiyati(t.sonFiyat())
                .enDusukAlisFiyati(t.enDusukFiyat())
                .enYuksekAlisFiyati(t.enYuksekFiyat())
                .sonAlisTarihi(t.sonTarih())
                .islemSayisi(t.islemSayisi())
                .build();
    }

    private SatisOzetDTO satisOzet(Long sirketId, Stok stok, LocalDate baslangic, LocalDate bitis) {
        HareketToplami t = toplam(satisOlcumleri(sirketId, stok.getId(), baslangic, bitis));
        return SatisOzetDTO.builder()
                .stokId(stok.getId()).stokAd(stok.getAd()).stokKodu(stok.getStokKodu())
                .toplamSatisMiktar(t.miktar())
                .toplamSatisTutari(t.tutar())
                .ortalamaBirimFiyat(t.ortalamaFiyat())
                .sonSatisFiyati(t.sonFiyat())
                .enDusukSatisFiyati(t.enDusukFiyat())
                .enYuksekSatisFiyati(t.enYuksekFiyat())
                .sonSatisTarihi(t.sonTarih())
                .islemSayisi(t.islemSayisi())
                .build();
    }

    private KarlilikDTO karlilik(Long sirketId, Stok stok, LocalDate baslangic, LocalDate bitis) {
        List<SatirOlcum> satisOlcumleri = satisOlcumleri(sirketId, stok.getId(), baslangic, bitis);
        HareketToplami satis = toplam(satisOlcumleri);

        BigDecimal ortalamaMaliyet = ortalamaMaliyet(stok);
        BigDecimal satisFiyat = satis.ortalamaFiyat();
        if (satisFiyat.signum() == 0 && stok.getSatisFiyati() != null) {
            satisFiyat = stok.getSatisFiyati();
        }
        BigDecimal stokMiktar = stok.getMiktar() == null ? BigDecimal.ZERO : stok.getMiktar();
        BigDecimal stokMaliyeti = ortalamaMaliyet.multiply(stokMiktar).setScale(PARA_OLCEGI, RoundingMode.HALF_UP);
        BigDecimal birimBrutKar = satisFiyat.subtract(ortalamaMaliyet).setScale(PARA_OLCEGI, RoundingMode.HALF_UP);

        BigDecimal toplamBrutKar = BigDecimal.ZERO;
        BigDecimal satisIadeTutari = BigDecimal.ZERO;
        for (SatirOlcum o : satisOlcumleri) {
            toplamBrutKar = toplamBrutKar.add(o.netBirimFiyat().subtract(ortalamaMaliyet).multiply(o.miktar()));
            if (o.miktar().signum() < 0) {
                satisIadeTutari = satisIadeTutari.add(o.netTutar().negate());
            }
        }
        toplamBrutKar = toplamBrutKar.setScale(PARA_OLCEGI, RoundingMode.HALF_UP);
        BigDecimal tavan = satis.tutar();
        BigDecimal brutKarMarji = tavan.signum() == 0
                ? BigDecimal.ZERO
                : toplamBrutKar.multiply(BigDecimal.valueOf(100)).divide(tavan, PARA_OLCEGI, RoundingMode.HALF_UP);

        return KarlilikDTO.builder()
                .stokId(stok.getId()).stokAd(stok.getAd()).stokKodu(stok.getStokKodu())
                .stokMiktar(stokMiktar)
                .ortalamaMaliyet(ortalamaMaliyet)
                .stokMaliyeti(stokMaliyeti)
                .ortalamaSatisFiyati(satisFiyat)
                .birimBrutKar(birimBrutKar)
                .toplamBrutKar(toplamBrutKar)
                .brutKarMarji(brutKarMarji)
                .satisIadeTutari(satisIadeTutari.setScale(PARA_OLCEGI, RoundingMode.HALF_UP))
                .satilanMiktar(satis.miktar().intValue())
                .build();
    }

    private BigDecimal ortalamaMaliyet(Stok stok) {
        BigDecimal fiyat = stok.getFiyat() == null ? BigDecimal.ZERO : stok.getFiyat();
        if (fiyat.signum() == 0 && stok.getTedarikciFiyat() != null) {
            return stok.getTedarikciFiyat().setScale(PARA_OLCEGI, RoundingMode.HALF_UP);
        }
        return fiyat.setScale(PARA_OLCEGI, RoundingMode.HALF_UP);
    }

    /**
     * Cari (tedarikçi/müşteri) bazlı özet. İadeler kendi türüne göre düşülür
     * (ALIS iadesi tedarikçi analizini, SATIS iadesi müşteri analizini azaltır).
     */
    private List<CariGrup> cariAnaliz(Long sirketId, Long stokId, Fatura.FaturaTur tur,
                                      LocalDate baslangic, LocalDate bitis) {
        Map<Long, List<SatirOlcum>> gruplar = new LinkedHashMap<>();
        for (FaturaAnalizSatirProjeksiyon s : faturaKalemRepository.analizSatirlari(
                stokId, sirketId, tur, Fatura.FaturaDurum.KESILDI, baslangic, bitis)) {
            if (s.getCariHesapId() == null) continue;
            BigDecimal adet = BigDecimal.valueOf(s.getAdet());
            gruplar.computeIfAbsent(s.getCariHesapId(), k -> new ArrayList<>())
                    .add(new SatirOlcum(adet, netFiyat(s.getBirimFiyat(), s.getIskontoOrani()),
                            netTutar(s.getBirimFiyat(), s.getIskontoOrani(), adet),
                            s.getFaturaTarihi(), s.getCariHesapAd()));
        }
        String iadeTur = tur == Fatura.FaturaTur.ALIS ? IADE_ALIS : IADE_SATIS;
        for (IadeAnalizSatirProjeksiyon i : iadeKalemRepository.analizSatirlari(
                stokId, sirketId, iadeTur, IADE_DURUM, baslangic, bitis)) {
            if (i.getCariHesapId() == null) continue;
            gruplar.computeIfAbsent(i.getCariHesapId(), k -> new ArrayList<>())
                    .add(new SatirOlcum(i.getMiktar().negate(), i.getBirimFiyat(),
                            i.getBirimFiyat().multiply(i.getMiktar()).negate().setScale(PARA_OLCEGI, RoundingMode.HALF_UP),
                            i.getIadeTarihi(), i.getCariHesapAd()));
        }
        List<CariGrup> sonuc = new ArrayList<>();
        for (Map.Entry<Long, List<SatirOlcum>> e : gruplar.entrySet()) {
            List<SatirOlcum> olcumler = e.getValue();
            HareketToplami t = toplam(olcumler);
            String ad = olcumler.stream().map(SatirOlcum::cariAd)
                    .filter(Objects::nonNull).findFirst().orElse(null);
            sonuc.add(new CariGrup(e.getKey(), ad, t.miktar(), t.tutar(), t.ortalamaFiyat(),
                    t.sonFiyat(), t.sonTarih(), t.islemSayisi()));
        }
        sonuc.sort(Comparator.comparing(CariGrup::toplamTutar).reversed());
        return sinirla(sonuc, CAri_LISTE_LIMITI);
    }

    private record CariGrup(Long cariHesapId, String cariHesapAd, BigDecimal toplamMiktar,
                            BigDecimal toplamTutar, BigDecimal ortalamaFiyat,
                            BigDecimal sonFiyat, LocalDate sonTarih, int islemSayisi) {
    }

    // ---------- Ölçüm toplama ----------

    private List<SatirOlcum> alisOlcumleri(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis) {
        List<SatirOlcum> liste = new ArrayList<>();
        for (FaturaAnalizSatirProjeksiyon s : faturaKalemRepository.analizSatirlari(
                stokId, sirketId, Fatura.FaturaTur.ALIS, Fatura.FaturaDurum.KESILDI, baslangic, bitis)) {
            BigDecimal adet = BigDecimal.valueOf(s.getAdet());
            liste.add(new SatirOlcum(adet, netFiyat(s.getBirimFiyat(), s.getIskontoOrani()),
                    netTutar(s.getBirimFiyat(), s.getIskontoOrani(), adet), s.getFaturaTarihi(), s.getCariHesapAd()));
        }
        for (IadeAnalizSatirProjeksiyon i : iadeKalemRepository.analizSatirlari(
                stokId, sirketId, IADE_ALIS, IADE_DURUM, baslangic, bitis)) {
            liste.add(new SatirOlcum(i.getMiktar().negate(), i.getBirimFiyat(),
                    i.getBirimFiyat().multiply(i.getMiktar()).negate().setScale(PARA_OLCEGI, RoundingMode.HALF_UP),
                    i.getIadeTarihi(), i.getCariHesapAd()));
        }
        return liste;
    }

    private List<SatirOlcum> satisOlcumleri(Long sirketId, Long stokId, LocalDate baslangic, LocalDate bitis) {
        List<SatirOlcum> liste = new ArrayList<>();
        for (FaturaAnalizSatirProjeksiyon s : faturaKalemRepository.analizSatirlari(
                stokId, sirketId, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, baslangic, bitis)) {
            BigDecimal adet = BigDecimal.valueOf(s.getAdet());
            liste.add(new SatirOlcum(adet, netFiyat(s.getBirimFiyat(), s.getIskontoOrani()),
                    netTutar(s.getBirimFiyat(), s.getIskontoOrani(), adet), s.getFaturaTarihi(), s.getCariHesapAd()));
        }
        for (IadeAnalizSatirProjeksiyon i : iadeKalemRepository.analizSatirlari(
                stokId, sirketId, IADE_SATIS, IADE_DURUM, baslangic, bitis)) {
            liste.add(new SatirOlcum(i.getMiktar().negate(), i.getBirimFiyat(),
                    i.getBirimFiyat().multiply(i.getMiktar()).negate().setScale(PARA_OLCEGI, RoundingMode.HALF_UP),
                    i.getIadeTarihi(), i.getCariHesapAd()));
        }
        return liste;
    }

    /** Tartılı özet: Σ(miktar×netBirimFiyat)/Σ(miktar). Fiyat istatistikleri yalnız olumlu (alım) satırlardan. */
    private HareketToplami toplam(List<SatirOlcum> olcumler) {
        BigDecimal miktar = BigDecimal.ZERO;
        BigDecimal tutar = BigDecimal.ZERO;
        BigDecimal min = null;
        BigDecimal max = null;
        BigDecimal son = null;
        LocalDate sonTarih = null;
        for (SatirOlcum o : olcumler) {
            miktar = miktar.add(o.miktar());
            tutar = tutar.add(o.netTutar());
            if (o.miktar().signum() > 0) {
                min = min == null ? o.netBirimFiyat() : min.min(o.netBirimFiyat());
                max = max == null ? o.netBirimFiyat() : max.max(o.netBirimFiyat());
                son = o.netBirimFiyat();
                sonTarih = o.tarih();
            } else if (son == null) {
                son = o.netBirimFiyat();
            }
        }
        BigDecimal ortalama = miktar.signum() == 0
                ? BigDecimal.ZERO
                : tutar.divide(miktar, PARA_OLCEGI, RoundingMode.HALF_UP);
        return new HareketToplami(miktar.setScale(PARA_OLCEGI, RoundingMode.HALF_UP),
                tutar.setScale(PARA_OLCEGI, RoundingMode.HALF_UP), ortalama,
                son, sonTarih, min, max, olcumler.size());
    }

    private BigDecimal netFiyat(BigDecimal birimFiyat, BigDecimal iskontoOrani) {
        BigDecimal birim = birimFiyat == null ? BigDecimal.ZERO : birimFiyat;
        BigDecimal iskonto = iskontoOrani == null ? BigDecimal.ZERO : iskontoOrani;
        if (iskonto.signum() == 0) {
            return birim.setScale(PARA_OLCEGI, RoundingMode.HALF_UP);
        }
        return birim.multiply(BigDecimal.valueOf(100).subtract(iskonto))
                .divide(BigDecimal.valueOf(100), PARA_OLCEGI, RoundingMode.HALF_UP);
    }

    private BigDecimal netTutar(BigDecimal birimFiyat, BigDecimal iskontoOrani, BigDecimal miktar) {
        return netFiyat(birimFiyat, iskontoOrani).multiply(miktar).setScale(PARA_OLCEGI, RoundingMode.HALF_UP);
    }

    private BigDecimal iNetTutar(IadeAnalizSatirProjeksiyon i) {
        return i.getBirimFiyat().multiply(i.getMiktar()).setScale(PARA_OLCEGI, RoundingMode.HALF_UP);
    }

    private static class AyToplami {
        private BigDecimal alisMiktar = BigDecimal.ZERO;
        private BigDecimal alisTutar = BigDecimal.ZERO;
        private BigDecimal satisMiktar = BigDecimal.ZERO;
        private BigDecimal satisTutar = BigDecimal.ZERO;

        void alisEkle(BigDecimal miktar, BigDecimal tutar) {
            alisMiktar = alisMiktar.add(miktar);
            alisTutar = alisTutar.add(tutar);
        }

        void satisEkle(BigDecimal miktar, BigDecimal tutar) {
            satisMiktar = satisMiktar.add(miktar);
            satisTutar = satisTutar.add(tutar);
        }

        BigDecimal alisOrtalama() {
            return islem(alisMiktar, alisTutar);
        }

        BigDecimal satisOrtalama() {
            return islem(satisMiktar, satisTutar);
        }

        private BigDecimal islem(BigDecimal miktar, BigDecimal tutar) {
            return miktar.signum() == 0
                    ? BigDecimal.ZERO
                    : tutar.divide(miktar, PARA_OLCEGI, RoundingMode.HALF_UP);
        }
    }
}