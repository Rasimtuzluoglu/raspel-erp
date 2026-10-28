package com.raspel.erp.service.finans;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.finans.ButceDTO;
import com.raspel.erp.dto.finans.ButceOzetDTO;
import com.raspel.erp.entity.finans.Butce;
import com.raspel.erp.entity.finans.Hareket;
import com.raspel.erp.entity.finans.Masraf;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.finans.ButceRepository;
import com.raspel.erp.repository.finans.ButceRevizyonRepository;
import com.raspel.erp.repository.finans.HareketRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ButceService {

    private final ButceRepository butceRepository;
    private final ButceRevizyonRepository butceRevizyonRepository;
    private final MasrafRepository masrafRepository;
    private final HareketRepository hareketRepository;
    private final TenantChecker tenantChecker;
    private final com.raspel.erp.service.sistem.AuditLogService auditLogService;
    private final com.raspel.erp.service.sistem.KategoriService kategoriService;

    @Transactional(readOnly = true)
    public Page<ButceDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return butceRepository.findBySirketIdOrderByYilDescAyDesc(sirketId, pageable).map(this::entityToDTO);
    }

    /** Bütçe listesi: yıl/ay/tür/kategori ve serbest metin filtreleriyle sunucu taraflı sayfalama. */
    @Transactional(readOnly = true)
    public Page<ButceDTO> filtreli(Long sirketId, Integer yil, Integer ay, String tur,
                                   String kategori, String durum, String q, Pageable pageable) {
        String qq = com.raspel.erp.util.AramaTemizleyici.like(q);
        String tt = turKodu(tur);
        String kk = (kategori == null || kategori.isBlank()) ? null : kategori.trim();
        String dd = (durum == null || durum.isBlank()) ? null : durum.trim().toUpperCase(Locale.ROOT);
        return butceRepository.filtreliGetir(sirketId, yil, ay, tt, dd, kk, qq, pageable).map(this::entityToDTO);
    }

    @Transactional(readOnly = true)
    public List<String> kategoriler(Long sirketId, Integer yil, String tur) {
        return butceRepository.kategoriler(sirketId, yil, turKodu(tur));
    }

    /** 12 aylık bütçe matrisi (kategori × ay). */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.finans.ButceMatrisDTO matris(Long sirketId, Integer yil, String tur) {
        int y = yil != null ? yil : LocalDate.now().getYear();
        String tt = turKodu(tur);
        List<Butce> liste = butceRepository.matrisSatirlari(sirketId, y, tt);

        Map<String, BigDecimal[]> map = new TreeMap<>();
        BigDecimal[] aylikToplam = new BigDecimal[12];
        for (int i = 0; i < 12; i++) aylikToplam[i] = BigDecimal.ZERO;
        BigDecimal genelToplam = BigDecimal.ZERO;

        for (Butce b : liste) {
            String kategori = (b.getKategori() != null && !b.getKategori().isBlank())
                    ? b.getKategori() : "(Kategorisiz)";
            BigDecimal[] aylar = map.computeIfAbsent(kategori, k -> {
                BigDecimal[] d = new BigDecimal[12];
                for (int i = 0; i < 12; i++) d[i] = BigDecimal.ZERO;
                return d;
            });
            int ay = (b.getAy() != null && b.getAy() >= 1 && b.getAy() <= 12) ? b.getAy() : 1;
            BigDecimal tutar = sifir(b.getTutar());
            aylar[ay - 1] = aylar[ay - 1].add(tutar);
            aylikToplam[ay - 1] = aylikToplam[ay - 1].add(tutar);
            genelToplam = genelToplam.add(tutar);
        }

        List<com.raspel.erp.dto.finans.ButceMatrisDTO.Satir> satirlar = map.entrySet().stream()
                .map(giris -> {
                    BigDecimal toplam = BigDecimal.ZERO;
                    for (BigDecimal v : giris.getValue()) toplam = toplam.add(v);
                    return com.raspel.erp.dto.finans.ButceMatrisDTO.Satir.builder()
                            .kategori(giris.getKey())
                            .aylar(giris.getValue())
                            .toplam(olcek(toplam))
                            .build();
                })
                .toList();

        return com.raspel.erp.dto.finans.ButceMatrisDTO.builder()
                .yil(y)
                .tur(tt)
                .satirlar(satirlar)
                .aylikToplam(aylikToplam)
                .genelToplam(olcek(genelToplam))
                .build();
    }

    /** Önceki yılın bütçelerini hedef yıla kopyalar; mevcut kategori+ay+tür atlanır. */
    @CacheEvict(value = "lookup", allEntries = true)
    public Map<String, Integer> kopyala(Long sirketId, Integer kaynakYil, Integer hedefYil, String tur) {
        if (kaynakYil == null || hedefYil == null) {
            throw new com.raspel.erp.exception.BusinessException("Kaynak ve hedef yıl zorunludur");
        }
        if (kaynakYil.equals(hedefYil)) {
            throw new com.raspel.erp.exception.BusinessException("Kaynak ve hedef yıl aynı olamaz");
        }
        String tt = turKodu(tur);
        List<Butce> kaynak = tt == null
                ? butceRepository.findBySirketIdAndYilOrderByKategoriAscAyAsc(sirketId, kaynakYil)
                : butceRepository.findBySirketIdAndYilAndTurOrderByKategoriAscAyAsc(sirketId, kaynakYil, tt);
        List<Butce> hedef = tt == null
                ? butceRepository.findBySirketIdAndYilOrderByKategoriAscAyAsc(sirketId, hedefYil)
                : butceRepository.findBySirketIdAndYilAndTurOrderByKategoriAscAyAsc(sirketId, hedefYil, tt);

        Set<String> mevcut = hedef.stream()
                .map(b -> b.getTur() + "|" + (b.getKategori() != null ? b.getKategori() : "") + "|" + b.getAy())
                .collect(Collectors.toSet());

        int kopyalanan = 0;
        int atlanan = 0;
        for (Butce b : kaynak) {
            String anahtar = b.getTur() + "|" + (b.getKategori() != null ? b.getKategori() : "") + "|" + b.getAy();
            if (mevcut.contains(anahtar)) {
                atlanan++;
                continue;
            }
            com.raspel.erp.entity.sistem.Kategori kategori = kategoriService.resolve(
                    sirketId, com.raspel.erp.entity.sistem.KategoriKapsam.BUTCE, b.getKategori());
            butceRepository.save(Butce.builder()
                    .ad(b.getAd())
                    .yil(hedefYil)
                    .ay(b.getAy())
                    .tutar(b.getTutar())
                    .tur(b.getTur())
                    .kategori(kategori != null ? kategori.getAd() : b.getKategori())
                    .kategoriId(kategori != null ? kategori.getId() : b.getKategoriId())
                    .sirketId(sirketId)
                    .aciklama("Kopya: " + kaynakYil + (b.getAciklama() != null ? " · " + b.getAciklama() : ""))
                    .durum("TASLAK")
                    .revizyon(1)
                    .build());
            mevcut.add(anahtar);
            kopyalanan++;
        }
        return Map.of("kopyalanan", kopyalanan, "atlanan", atlanan);
    }

    /**
     * KPI özeti. Gider gerçekleşeni masraf kayıtlarından, gelir gerçekleşeni
     * iptal edilmemiş tahsilat hareketlerinden hesaplanır.
     */
    @Transactional(readOnly = true)
    public ButceOzetDTO ozet(Long sirketId, Integer yil, String tur) {
        int y = yil != null ? yil : LocalDate.now().getYear();
        String tt = turKodu(tur);
        BigDecimal gelirButce = sifir(butceRepository.toplamTutar(sirketId, y, "GELIR"));
        BigDecimal giderButce = sifir(butceRepository.toplamTutar(sirketId, y, "GIDER"));

        LocalDate bas = LocalDate.of(y, 1, 1);
        LocalDate bit = LocalDate.of(y, 12, 31);
        BigDecimal gelirGerceklesen = sifir(hareketRepository.sumTutarBySirketIdAndTurAndTarihBetween(
                sirketId, Hareket.HareketTuru.TAHSILAT, bas, bit));
        BigDecimal giderGerceklesen = sifir(masrafRepository.sumTutarBySirketIdAndTarihBetween(sirketId, bas, bit));

        BigDecimal butce;
        BigDecimal gerceklesen;
        if ("GELIR".equals(tt)) {
            butce = gelirButce;
            gerceklesen = gelirGerceklesen;
        } else if ("GIDER".equals(tt)) {
            butce = giderButce;
            gerceklesen = giderGerceklesen;
        } else {
            butce = gelirButce.add(giderButce);
            gerceklesen = gelirGerceklesen.add(giderGerceklesen);
        }
        BigDecimal sapma = gerceklesen.subtract(butce);
        BigDecimal kullanim = butce.compareTo(BigDecimal.ZERO) > 0
                ? gerceklesen.multiply(BigDecimal.valueOf(100)).divide(butce, 1, RoundingMode.HALF_UP)
                : null;

        return ButceOzetDTO.builder()
                .butceToplam(olcek(butce))
                .gerceklesenToplam(olcek(gerceklesen))
                .sapma(olcek(sapma))
                .kullanimYuzde(kullanim)
                .gelirButce(olcek(gelirButce))
                .giderButce(olcek(giderButce))
                .gelirGerceklesen(olcek(gelirGerceklesen))
                .giderGerceklesen(olcek(giderGerceklesen))
                .kayitSayisi(butceRepository.sayi(sirketId, y, tt))
                .kategoriSayisi(butceRepository.kategoriler(sirketId, y, tt).size())
                .onayBekleyen(butceRepository.countOnayBekleyen(sirketId, y))
                .build();
    }

    /**
     * Tür ayrımlı bütçe–gerçekleşen raporu.
     *
     * <p>Gider: bütçe kategorileri masraf kategorileriyle eşleştirilir.
     * Gelir: tahsilat hareketlerinde kategori alanı olmadığı için toplam
     * tahsilat tek satırda gösterilir; kategori satırları plan tutarını taşır.
     */
    @Transactional(readOnly = true)
    public com.raspel.erp.dto.finans.ButceGerceklesenRaporDTO gerceklesen(
            Long sirketId, Integer yil, Integer ay, String tur) {
        int y = yil != null ? yil : LocalDate.now().getYear();
        String tt = turKodu(tur) != null ? turKodu(tur) : "GIDER";
        LocalDate bas = ay != null ? LocalDate.of(y, ay, 1) : LocalDate.of(y, 1, 1);
        LocalDate bit = ay != null ? YearMonth.of(y, ay).atEndOfMonth() : LocalDate.of(y, 12, 31);

        List<Butce> butceler = butceRepository
                .findBySirketIdOrderByYilDescAyDesc(sirketId, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(b -> b.getYil() != null && b.getYil() == y)
                .filter(b -> ay == null || (b.getAy() != null && b.getAy().equals(ay)))
                .filter(b -> tt.equals(b.getTur()))
                .filter(b -> "ONAYLANDI".equals(b.getDurum()))
                .toList();

        Map<String, BigDecimal> butceHaritasi = butceler.stream()
                .filter(b -> b.getKategori() != null && !b.getKategori().isBlank())
                .collect(Collectors.groupingBy(Butce::getKategori,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Butce::getTutar, BigDecimal::add)));
        BigDecimal butceToplam = butceler.stream()
                .map(b -> sifir(b.getTutar()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<com.raspel.erp.dto.finans.ButceGerceklesenSatirDTO> satirlar = new ArrayList<>();
        BigDecimal gerceklesenToplam;

        if ("GELIR".equals(tt)) {
            gerceklesenToplam = sifir(hareketRepository.sumTutarBySirketIdAndTurAndTarihBetween(
                    sirketId, Hareket.HareketTuru.TAHSILAT, bas, bit));
            BigDecimal sapma = gerceklesenToplam.subtract(butceToplam);
            satirlar.add(com.raspel.erp.dto.finans.ButceGerceklesenSatirDTO.builder()
                    .kategori("Tahsilat")
                    .butce(olcek(butceToplam))
                    .gerceklesen(olcek(gerceklesenToplam))
                    .sapma(olcek(sapma))
                    .kullanimYuzdesi(kullanim(butceToplam, gerceklesenToplam))
                    .build());
            butceHaritasi.forEach((kategori, tutar) -> satirlar.add(
                    com.raspel.erp.dto.finans.ButceGerceklesenSatirDTO.builder()
                            .kategori(kategori)
                            .butce(olcek(tutar))
                            .build()));
        } else {
            Map<String, BigDecimal> gerceklesenHaritasi = masrafRepository
                    .findBySirketIdAndTarihBetween(sirketId, bas, bit).stream()
                    .filter(m -> m.getKategori() != null && !m.getKategori().isBlank())
                    .collect(Collectors.groupingBy(Masraf::getKategori,
                            TreeMap::new,
                            Collectors.reducing(BigDecimal.ZERO, Masraf::getTutar, BigDecimal::add)));
            gerceklesenToplam = gerceklesenHaritasi.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Set<String> kategoriler = new TreeSet<>(butceHaritasi.keySet());
            kategoriler.addAll(gerceklesenHaritasi.keySet());
            for (String kategori : kategoriler) {
                BigDecimal butce = butceHaritasi.getOrDefault(kategori, BigDecimal.ZERO);
                BigDecimal gerceklesen = gerceklesenHaritasi.getOrDefault(kategori, BigDecimal.ZERO);
                satirlar.add(com.raspel.erp.dto.finans.ButceGerceklesenSatirDTO.builder()
                        .kategori(kategori)
                        .butce(olcek(butce))
                        .gerceklesen(olcek(gerceklesen))
                        .sapma(olcek(gerceklesen.subtract(butce)))
                        .kullanimYuzdesi(kullanim(butce, gerceklesen))
                        .build());
            }
        }

        BigDecimal sapma = gerceklesenToplam.subtract(butceToplam);
        return com.raspel.erp.dto.finans.ButceGerceklesenRaporDTO.builder()
                .tur(tt)
                .yil(y)
                .ay(ay)
                .baslangic(bas)
                .bitis(bit)
                .butceToplam(olcek(butceToplam))
                .gerceklesenToplam(olcek(gerceklesenToplam))
                .sapma(olcek(sapma))
                .kullanimYuzde(kullanim(butceToplam, gerceklesenToplam))
                .satirlar(satirlar)
                .build();
    }

    private static BigDecimal kullanim(BigDecimal butce, BigDecimal gerceklesen) {
        return sifir(butce).compareTo(BigDecimal.ZERO) > 0
                ? sifir(gerceklesen).multiply(BigDecimal.valueOf(100)).divide(butce, 1, RoundingMode.HALF_UP)
                : null;
    }

    private static String turKodu(String tur) {        if (tur == null || tur.isBlank()) return null;
        String t = tur.trim().toUpperCase(Locale.ROOT);
        return ("GELIR".equals(t) || "GIDER".equals(t)) ? t : null;
    }

    private static BigDecimal sifir(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal olcek(BigDecimal v) {
        return sifir(v).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public ButceDTO getir(Long id) {
        Butce b = butceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Butce", id));
        tenantChecker.check(b.getSirketId(), "Butce");
        return entityToDTO(b);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public ButceDTO olustur(ButceDTO dto, Long sirketId) {
        com.raspel.erp.entity.sistem.Kategori kategori = kategoriService.resolve(
                sirketId, com.raspel.erp.entity.sistem.KategoriKapsam.BUTCE, dto.getKategori());
        Butce butce = Butce.builder()
                .ad(dto.getAd())
                .yil(dto.getYil())
                .ay(dto.getAy())
                .tutar(dto.getTutar())
                .tur(dto.getTur())
                .kategori(kategori != null ? kategori.getAd() : dto.getKategori())
                .kategoriId(kategori != null ? kategori.getId() : null)
                .sirketId(sirketId)
                .aciklama(dto.getAciklama())
                .durum("TASLAK")
                .revizyon(1)
                .build();
        Butce kayit = butceRepository.save(butce);
        revizyonYaz(kayit, "OLUSTUR", "Bütçe oluşturuldu", null);
        return entityToDTO(kayit);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public ButceDTO guncelle(Long id, ButceDTO dto) {
        Butce butce = butceGetir(id);
        if (dto.getAd() != null) butce.setAd(dto.getAd());
        if (dto.getYil() != null) butce.setYil(dto.getYil());
        if (dto.getAy() != null) butce.setAy(dto.getAy());
        if (dto.getTutar() != null) butce.setTutar(dto.getTutar());
        if (dto.getTur() != null) butce.setTur(dto.getTur());
        if (dto.getKategori() != null) {
            com.raspel.erp.entity.sistem.Kategori kategori = kategoriService.resolve(
                    butce.getSirketId(), com.raspel.erp.entity.sistem.KategoriKapsam.BUTCE, dto.getKategori());
            butce.setKategori(kategori != null ? kategori.getAd() : dto.getKategori());
            butce.setKategoriId(kategori != null ? kategori.getId() : null);
        }
        if (dto.getAciklama() != null) butce.setAciklama(dto.getAciklama());
        butce.setRevizyon((butce.getRevizyon() != null ? butce.getRevizyon() : 1) + 1);
        // Onaylı/reddedilmiş bütçe değişirse yeniden onaya düşer.
        if (!"TASLAK".equals(butce.getDurum())) {
            butce.setDurum("TASLAK");
            butce.setOnaylayanKullaniciId(null);
            butce.setOnayTarihi(null);
            butce.setRedGerekcesi(null);
        }
        Butce kayit = butceRepository.save(butce);
        revizyonYaz(kayit, "GUNCELLE", "Bütçe güncellendi", null);
        return entityToDTO(kayit);
    }

    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Butce b = butceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Butce", id));
        tenantChecker.check(b.getSirketId(), "Butce");
        auditLogService.finansalSilmeLog("Butce", id,
                "Bütçe silindi: " + b.getTutar() + " TL - " + (b.getAd() != null ? b.getAd() : "")
                        + (b.getYil() != null ? " (" + b.getYil() + ")" : ""));
        butceRepository.deleteById(id);
    }

    /** Taslak bütçeyi onaylar. */
    @CacheEvict(value = "lookup", allEntries = true)
    public ButceDTO onayla(Long id, Long kullaniciId) {
        Butce butce = butceGetir(id);
        if (!"TASLAK".equals(butce.getDurum())) {
            throw new com.raspel.erp.exception.BusinessException(
                    "Yalnızca taslak bütçe onaylanabilir (durum: " + butce.getDurum() + ")");
        }
        butce.setDurum("ONAYLANDI");
        butce.setOnaylayanKullaniciId(kullaniciId);
        butce.setOnayTarihi(java.time.LocalDateTime.now());
        butce.setRedGerekcesi(null);
        Butce kayit = butceRepository.save(butce);
        revizyonYaz(kayit, "ONAY", "Bütçe onaylandı", kullaniciId);
        return entityToDTO(kayit);
    }

    /** Taslak bütçeyi gerekçesiyle reddeder. */
    @CacheEvict(value = "lookup", allEntries = true)
    public ButceDTO reddet(Long id, String gerekce, Long kullaniciId) {
        Butce butce = butceGetir(id);
        if (!"TASLAK".equals(butce.getDurum())) {
            throw new com.raspel.erp.exception.BusinessException(
                    "Yalnızca taslak bütçe reddedilebilir (durum: " + butce.getDurum() + ")");
        }
        String aciklama = (gerekce == null || gerekce.isBlank()) ? "Gerekçe belirtilmedi" : gerekce.trim();
        butce.setDurum("REDDEDILDI");
        butce.setRedGerekcesi(aciklama);
        butce.setOnaylayanKullaniciId(kullaniciId);
        butce.setOnayTarihi(java.time.LocalDateTime.now());
        Butce kayit = butceRepository.save(butce);
        revizyonYaz(kayit, "RED", aciklama, kullaniciId);
        return entityToDTO(kayit);
    }

    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.finans.ButceRevizyonDTO> revizyonlar(Long id) {
        Butce butce = butceGetir(id);
        return butceRevizyonRepository.findByButceIdOrderByOlusturmaTarihiAscIdAsc(butce.getId()).stream()
                .map(r -> com.raspel.erp.dto.finans.ButceRevizyonDTO.builder()
                        .id(r.getId())
                        .butceId(r.getButceId())
                        .revizyon(r.getRevizyon())
                        .islem(r.getIslem())
                        .durum(r.getDurum())
                        .aciklama(r.getAciklama())
                        .tutar(r.getTutar())
                        .degistirenKullaniciId(r.getDegistirenKullaniciId())
                        .olusturmaTarihi(r.getOlusturmaTarihi())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ButceDTO> onayBekleyenler(Long sirketId, Integer yil) {
        return butceRepository.onayBekleyenler(sirketId, yil).stream()
                .map(this::entityToDTO)
                .toList();
    }

    private Butce butceGetir(Long id) {
        Butce butce = butceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Butce", id));
        tenantChecker.check(butce.getSirketId(), "Butce");
        return butce;
    }

    private void revizyonYaz(Butce b, String islem, String aciklama, Long kullaniciId) {
        butceRevizyonRepository.save(com.raspel.erp.entity.finans.ButceRevizyon.builder()
                .butceId(b.getId())
                .sirketId(b.getSirketId())
                .revizyon(b.getRevizyon())
                .ad(b.getAd())
                .kategori(b.getKategori())
                .yil(b.getYil())
                .ay(b.getAy())
                .tutar(b.getTutar())
                .tur(b.getTur())
                .durum(b.getDurum())
                .islem(islem)
                .aciklama(aciklama)
                .degistirenKullaniciId(kullaniciId)
                .build());
    }

    private ButceDTO entityToDTO(Butce b) {
        return ButceDTO.builder()
                .id(b.getId()).ad(b.getAd()).yil(b.getYil()).ay(b.getAy())
                .tutar(b.getTutar()).tur(b.getTur()).kategori(b.getKategori())
                .kategoriId(b.getKategoriId())
                .sirketId(b.getSirketId()).aciklama(b.getAciklama())
                .olusturmaTarihi(b.getOlusturmaTarihi())
                .durum(b.getDurum())
                .revizyon(b.getRevizyon())
                .onaylayanKullaniciId(b.getOnaylayanKullaniciId())
                .onayTarihi(b.getOnayTarihi())
                .redGerekcesi(b.getRedGerekcesi())
                .build();
    }
}
