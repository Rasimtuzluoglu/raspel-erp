package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.*;
import com.raspel.erp.entity.sistem.Kategori;
import com.raspel.erp.entity.sistem.KategoriKapsam;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.ButceRepository;
import com.raspel.erp.repository.finans.KasaHareketRepository;
import com.raspel.erp.repository.finans.MasrafRepository;
import com.raspel.erp.repository.sistem.KategoriRepository;
import com.raspel.erp.repository.ticaret.IskontoKuraliRepository;
import com.raspel.erp.util.AramaTemizleyici;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Merkezi kategori registry servisi.
 *
 * <p>Sorumluluklar:
 * <ul>
 *   <li>Kapsam bazlı listeleme, arama, 2 seviyeli ağaç ve KPI özeti</li>
 *   <li>CRUD + doğrulama (ad tekrarı, kapsam, hiyerarşi, palet/ikon/sıra)</li>
 *   <li>Kullanım sayımı, silme engeli, arşiv/aktifleştirme, birleştirme</li>
 *   <li>Legacy string kolonların (stok/masraf/bütçe/iskonto) senkron tutulması</li>
 *   <li>Domain servisleri için get-or-create ({@link #resolve})</li>
 * </ul>
 *
 * <p>Tenant izolasyonu her işlemde {@link TenantChecker} ile doğrulanır.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KategoriService {

    /** Sabit renk paleti (frontend ile aynı). */
    public static final List<String> RENK_PALETI = List.of(
            "#2563EB", "#059669", "#DC2626", "#D97706", "#7C3AED", "#DB2777",
            "#0891B2", "#65A30D", "#EA580C", "#4F46E5", "#64748B", "#0F172A");

    /** PrimeIcons sınıfı deseni. */
    private static final Pattern IKON_DESENI = Pattern.compile("^pi pi-[a-z0-9-]+$");

    /** Personel masraf kodları — sabittir, registry'ye yazılmaz (salt-okunur). */
    private static final List<SistemKategoriDTO> SISTEM_KATEGORILERI = List.of(
            new SistemKategoriDTO("YAKIT", "Yakıt", "MASRAF"),
            new SistemKategoriDTO("YEMEK", "Yemek", "MASRAF"),
            new SistemKategoriDTO("KONAKLAMA", "Konaklama", "MASRAF"),
            new SistemKategoriDTO("ULASIM", "Ulaşım", "MASRAF"),
            new SistemKategoriDTO("MALZEME", "Malzeme", "MASRAF"),
            new SistemKategoriDTO("DIGER", "Diğer", "MASRAF"));

    private final KategoriRepository kategoriRepository;
    private final StokRepository stokRepository;
    private final MasrafRepository masrafRepository;
    private final ButceRepository butceRepository;
    private final IskontoKuraliRepository iskontoKuraliRepository;
    private final KasaHareketRepository kasaHareketRepository;
    private final TenantChecker tenantChecker;

    // ---------------------------------------------------------------
    // Okuma
    // ---------------------------------------------------------------

    /** Geriye uyumlu düz liste (kapsam verilmezse tüm kapsamlar). */
    @Transactional(readOnly = true)
    public Page<KategoriDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        return listele(sirketId, null, null, null, null, pageable);
    }

    /**
     * Sayfalı düz liste. {@code q} boşsa filtre uygulanmaz; 1 karakterlik arama
     * (diğer ekranlarla tutarlı olarak) boş sonuç döner.
     */
    @Transactional(readOnly = true)
    public Page<KategoriDTO> listele(Long sirketId, KategoriKapsam kapsam, String q,
                                     Boolean aktif, Long ustKategoriId, Pageable pageable) {
        if (sirketId == null) return Page.empty(pageable);
        String desen = AramaTemizleyici.like(q);
        Page<Kategori> sayfa = kapsam == null
                ? kategoriRepository.listeleTumKapsamlar(sirketId, desen, aktif, ustKategoriId, pageable)
                : kategoriRepository.listele(sirketId, kapsam, desen, aktif, ustKategoriId, pageable);
        Map<Long, Long> kullanim = kullanimHaritasi(sirketId, kapsam);
        Map<Long, Long> altSayilari = altKategoriSayilari(sirketId);
        return sayfa.map(k -> entityToDTO(k, kullanim, altSayilari));
    }

    /** 2 seviyeli ağaç; alt kategoriler üst kategorinin altında döner. */
    @Transactional(readOnly = true)
    public List<KategoriAgacDTO> agac(Long sirketId, KategoriKapsam kapsam, String q, Boolean aktif) {
        if (sirketId == null) return List.of();
        List<Kategori> liste = kapsam == null
                ? kategoriRepository.findBySirketIdOrderByKapsamAscSiraAscAdAsc(sirketId)
                : kategoriRepository.findBySirketIdAndKapsamOrderBySiraAscAdAsc(sirketId, kapsam);
        Map<Long, Long> kullanim = kullanimHaritasi(sirketId, kapsam);
        List<Kategori> filtreli = filtrele(liste, q, aktif);
        return agacKur(filtreli, liste, kullanim, false);
    }

    /** Aktif kategorilerin üst kategoriye göre gruplu hâli (dropdown). */
    @Cacheable(value = "lookup", key = "'kategoriSecenek:' + #sirketId + ':' + (#kapsam == null ? 'TUMU' : #kapsam.name())")
    @Transactional(readOnly = true)
    public List<KategoriAgacDTO> secenekler(Long sirketId, KategoriKapsam kapsam) {
        if (sirketId == null) return List.of();
        List<Kategori> liste = kapsam == null
                ? kategoriRepository.findBySirketIdOrderByKapsamAscSiraAscAdAsc(sirketId)
                : kategoriRepository.findBySirketIdAndKapsamOrderBySiraAscAdAsc(sirketId, kapsam);
        List<Kategori> aktifler = liste.stream()
                .filter(k -> Boolean.TRUE.equals(k.getAktif()))
                .toList();
        Map<Long, Long> kullanim = kullanimHaritasi(sirketId, kapsam);
        // Üstü pasif olan alt kategoriler de listelenmez (tek başına anlamsız).
        List<Kategori> gorunur = aktifler.stream()
                .filter(k -> k.getUstKategori() == null
                        || aktifler.stream().anyMatch(a -> Objects.equals(a.getId(), k.getUstKategori().getId())))
                .toList();
        return agacKur(gorunur, liste, kullanim, true);
    }

    /** KPI özeti: toplam / aktif / pasif / alt kategori. */
    @Cacheable(value = "lookup", key = "'kategoriOzet:' + #sirketId + ':' + (#kapsam == null ? 'TUMU' : #kapsam.name())")
    @Transactional(readOnly = true)
    public KategoriOzetDTO ozet(Long sirketId, KategoriKapsam kapsam) {
        if (sirketId == null) return new KategoriOzetDTO(0, 0, 0, 0);
        long toplam = 0;
        long aktif = 0;
        long alt = 0;
        if (kapsam == null) {
            for (KategoriKapsam k : KategoriKapsam.values()) {
                toplam += kategoriRepository.countBySirketIdAndKapsam(sirketId, k);
                aktif += kategoriRepository.countBySirketIdAndKapsamAndAktif(sirketId, k, true);
                alt += kategoriRepository.countBySirketIdAndKapsamAndUstKategoriIdIsNotNull(sirketId, k);
            }
        } else {
            toplam = kategoriRepository.countBySirketIdAndKapsam(sirketId, kapsam);
            aktif = kategoriRepository.countBySirketIdAndKapsamAndAktif(sirketId, kapsam, true);
            alt = kategoriRepository.countBySirketIdAndKapsamAndUstKategoriIdIsNotNull(sirketId, kapsam);
        }
        return new KategoriOzetDTO(toplam, aktif, toplam - aktif, alt);
    }

    @Transactional(readOnly = true)
    public KategoriDTO getir(Long id) {
        Kategori k = getEntity(id);
        return entityToDTO(k,
                kullanimHaritasi(k.getSirketId(), k.getKapsam()),
                altKategoriSayilari(k.getSirketId()));
    }

    /** Modül bazlı kullanım sayıları (silme engeli ve birleştirme özeti). */
    @Transactional(readOnly = true)
    public KategoriKullanimDTO kullanim(Long id) {
        Kategori k = getEntity(id);
        List<KategoriKullanimDTO.Detay> detaylar = new ArrayList<>();
        long stok = 0, kasa = 0, masraf = 0, butce = 0, iskonto = 0;
        switch (k.getKapsam()) {
            case KASA_GELIR, KASA_GIDER -> kasa = kasaHareketRepository.countByKategoriId(id);
            case STOK -> {
                stok = stokRepository.countByKategoriId(id);
                iskonto = iskontoKuraliRepository.countByKategoriId(id);
            }
            case MASRAF -> masraf = masrafRepository.countByKategoriId(id);
            case BUTCE -> butce = butceRepository.countByKategoriId(id);
        }
        if (stok > 0) detaylar.add(new KategoriKullanimDTO.Detay("STOK", stok));
        if (kasa > 0) detaylar.add(new KategoriKullanimDTO.Detay("KASA", kasa));
        if (masraf > 0) detaylar.add(new KategoriKullanimDTO.Detay("MASRAF", masraf));
        if (butce > 0) detaylar.add(new KategoriKullanimDTO.Detay("BUTCE", butce));
        if (iskonto > 0) detaylar.add(new KategoriKullanimDTO.Detay("ISKONTO", iskonto));
        long toplam = stok + kasa + masraf + butce + iskonto;
        return KategoriKullanimDTO.builder()
                .kategoriId(k.getId()).ad(k.getAd()).toplam(toplam).detaylar(detaylar)
                .build();
    }

    /** Personel masraf kodları (salt-okunur, sabit). */
    @Transactional(readOnly = true)
    public List<SistemKategoriDTO> sistemKategorileri() {
        return SISTEM_KATEGORILERI;
    }

    /**
     * Geriye uyumluluk: {@code GELIR}/{@code GIDER} (veya {@code KASA_GELIR}/
     * {@code KASA_GIDER}) türüne göre kasa kategorileri.
     */
    @Cacheable(value = "lookup", sync = true, key = "'kategoriTur:' + #sirketId + ':' + #tur")
    @Transactional(readOnly = true)
    public List<KategoriDTO> turuGetir(String tur, Long sirketId) {
        KategoriKapsam kapsam = KategoriKapsam.turdan(tur);
        if (kapsam == null || sirketId == null) return List.of();
        Map<Long, Long> kullanim = kullanimHaritasi(sirketId, kapsam);
        Map<Long, Long> altSayilari = altKategoriSayilari(sirketId);
        return kategoriRepository.findBySirketIdAndKapsamOrderBySiraAscAdAsc(sirketId, kapsam).stream()
                .map(k -> entityToDTO(k, kullanim, altSayilari))
                .toList();
    }

    // ---------------------------------------------------------------
    // Yazma
    // ---------------------------------------------------------------

    @CacheEvict(value = "lookup", allEntries = true)
    public KategoriDTO olustur(KategoriDTO dto, Long sirketId) {
        if (sirketId == null) throw new BusinessException("Şirket bilgisi zorunludur");
        KategoriKapsam kapsam = kapsamCoz(dto.getKapsam(), dto.getTur());
        String ad = normaliseAd(kapsam, dto.getAd());
        if (ad == null || ad.isBlank()) throw new BusinessException("Kategori adı boş olamaz");
        renkDogrula(dto.getRenk());
        ikonDogrula(dto.getIkon());
        siraDogrula(dto.getSira());
        if (kategoriRepository.existsBySirketIdAndKapsamAndAdIgnoreCase(sirketId, kapsam, ad)) {
            throw new BusinessException("Bu kapsamda aynı adlı kategori zaten var: " + ad);
        }
        Kategori ust = ustKategoriCoz(dto.getUstKategoriId(), sirketId, kapsam, null);
        Kategori k = Kategori.builder()
                .sirketId(sirketId)
                .kapsam(kapsam)
                .ad(ad)
                .ustKategori(ust)
                .renk(bosIseNull(dto.getRenk()))
                .ikon(bosIseNull(dto.getIkon()))
                .sira(dto.getSira() != null ? dto.getSira() : 0)
                .aktif(dto.getAktif() != null ? dto.getAktif() : Boolean.TRUE)
                .aciklama(bosIseNull(dto.getAciklama()))
                .build();
        Kategori kaydedilen = kategoriRepository.save(k);
        return entityToDTO(kaydedilen, kullanimHaritasi(sirketId, kapsam),
                altKategoriSayilari(sirketId));
    }

    /**
     * Kategori güncelleme. Ad değişirse bağlı legacy kolonlar (stok/masraf/
     * bütçe/iskonto) tek transaction'da güncellenir.
     */
    @CacheEvict(value = "lookup", allEntries = true)
    public KategoriDTO guncelle(Long id, KategoriDTO dto) {
        Kategori k = getEntity(id);
        tenantChecker.check(k.getSirketId(), "Kategori");

        KategoriKapsam yeniKapsam = (dto.getKapsam() != null || dto.getTur() != null)
                ? kapsamCoz(dto.getKapsam(), dto.getTur())
                : k.getKapsam();
        boolean kapsamDegisti = yeniKapsam != k.getKapsam();
        if (kapsamDegisti) {
            if (!kategoriRepository.findByUstKategoriId(id).isEmpty()) {
                throw new BusinessException("Alt kategorisi olan kategorinin kapsamı değiştirilemez");
            }
            if (kullanimSayisi(k) > 0) {
                throw new BusinessException("Kullanımda olan kategorinin kapsamı değiştirilemez");
            }
            k.setKapsam(yeniKapsam);
            k.setUstKategori(null);
        }

        String eskiAd = k.getAd();
        String yeniAd = dto.getAd() != null
                ? normaliseAd(yeniKapsam, dto.getAd())
                : normaliseAd(yeniKapsam, eskiAd);
        if (yeniAd == null || yeniAd.isBlank()) throw new BusinessException("Kategori adı boş olamaz");
        boolean adDegisti = !yeniAd.equalsIgnoreCase(eskiAd);
        if (adDegisti && kategoriRepository
                .findBySirketIdAndKapsamAndAdIgnoreCaseAndIdNot(k.getSirketId(), yeniKapsam, yeniAd, id)
                .isPresent()) {
            throw new BusinessException("Bu kapsamda aynı adlı kategori zaten var: " + yeniAd);
        }

        renkDogrula(dto.getRenk());
        ikonDogrula(dto.getIkon());
        siraDogrula(dto.getSira());

        k.setAd(yeniAd);
        // PUT semantiği: ustKategoriId null ise kök kategoriye taşınır.
        Kategori ust = ustKategoriCoz(dto.getUstKategoriId(), k.getSirketId(), yeniKapsam, id);
        k.setUstKategori(ust);
        if (dto.getRenk() != null) k.setRenk(bosIseNull(dto.getRenk()));
        if (dto.getIkon() != null) k.setIkon(bosIseNull(dto.getIkon()));
        if (dto.getSira() != null) k.setSira(dto.getSira());
        if (dto.getAktif() != null) k.setAktif(dto.getAktif());
        if (dto.getAciklama() != null) k.setAciklama(bosIseNull(dto.getAciklama()));

        Kategori kaydedilen = kategoriRepository.save(k);

        if (adDegisti) {
            legacyAdYayilim(kaydedilen);
        }
        return entityToDTO(kaydedilen, kullanimHaritasi(k.getSirketId(), yeniKapsam),
                altKategoriSayilari(k.getSirketId()));
    }

    /** Silme: kullanım veya alt kategori varsa reddedilir (mesajda adet). */
    @CacheEvict(value = "lookup", allEntries = true)
    public void sil(Long id) {
        Kategori k = getEntity(id);
        tenantChecker.check(k.getSirketId(), "Kategori");
        long kullanim = kullanimSayisi(k);
        if (kullanim > 0) {
            throw new BusinessException(kullanim + " kayıtta kullanılıyor; bunun yerine arşivleyin");
        }
        long altKategori = kategoriRepository.findByUstKategoriId(id).size();
        if (altKategori > 0) {
            throw new BusinessException(altKategori
                    + " alt kategori var; önce onları silin veya başka kategoriye taşıyın");
        }
        kategoriRepository.delete(k);
    }

    /** Arşivle: aktif alt kategorisi varsa reddedilir. */
    @CacheEvict(value = "lookup", allEntries = true)
    public KategoriDTO arsivle(Long id) {
        Kategori k = getEntity(id);
        tenantChecker.check(k.getSirketId(), "Kategori");
        long aktifAlt = kategoriRepository.countByUstKategoriIdAndAktifTrue(id);
        if (aktifAlt > 0) {
            throw new BusinessException("Aktif alt kategorisi olan kategori arşivlenemez ("
                    + aktifAlt + " adet)");
        }
        k.setAktif(false);
        return entityToDTO(kategoriRepository.save(k),
                kullanimHaritasi(k.getSirketId(), k.getKapsam()),
                altKategoriSayilari(k.getSirketId()));
    }

    /** Arşivden çıkar: pasif üst kategori varsa reddedilir. */
    @CacheEvict(value = "lookup", allEntries = true)
    public KategoriDTO aktiflestir(Long id) {
        Kategori k = getEntity(id);
        tenantChecker.check(k.getSirketId(), "Kategori");
        if (k.getUstKategori() != null && !Boolean.TRUE.equals(k.getUstKategori().getAktif())) {
            throw new BusinessException("Üst kategori pasif; önce üst kategoriyi aktifleştirin");
        }
        k.setAktif(true);
        return entityToDTO(kategoriRepository.save(k),
                kullanimHaritasi(k.getSirketId(), k.getKapsam()),
                altKategoriSayilari(k.getSirketId()));
    }

    /**
     * İki kategoriyi birleştirir: kaynağın alt kategorileri ve tüm bağlı
     * kayıtlar (kasa/stok/masraf/bütçe/iskonto) hedefe taşınır, kaynak arşivlenir.
     */
    @CacheEvict(value = "lookup", allEntries = true)
    public KategoriDTO birlestir(Long kaynakId, Long hedefId) {
        if (kaynakId == null || hedefId == null) {
            throw new BusinessException("Kaynak ve hedef kategori zorunludur");
        }
        if (kaynakId.equals(hedefId)) {
            throw new BusinessException("Kaynak ve hedef kategori aynı olamaz");
        }
        Kategori kaynak = getEntity(kaynakId);
        Kategori hedef = getEntity(hedefId);
        tenantChecker.check(kaynak.getSirketId(), "Kategori");
        tenantChecker.check(hedef.getSirketId(), "Kategori");
        if (kaynak.getKapsam() != hedef.getKapsam()) {
            throw new BusinessException("Farklı kapsamdaki kategoriler birleştirilemez");
        }
        if (!Boolean.TRUE.equals(hedef.getAktif())) {
            throw new BusinessException("Hedef kategori aktif olmalıdır");
        }
        if (hedef.getUstKategori() != null) {
            throw new BusinessException("Hedef kategori üst kategori olmalıdır (alt kategori olamaz)");
        }

        // Kaynağın alt kategorileri hedefe taşınır.
        List<Kategori> cocuklar = kategoriRepository.findByUstKategoriId(kaynakId);
        for (Kategori cocuk : cocuklar) {
            cocuk.setUstKategori(hedef);
        }
        if (!cocuklar.isEmpty()) {
            kategoriRepository.saveAll(cocuklar);
        }

        String hedefAd = hedef.getAd();
        switch (kaynak.getKapsam()) {
            case STOK -> {
                stokRepository.kategoriBirlestir(kaynakId, hedefId, hedefAd);
                iskontoKuraliRepository.kategoriBirlestir(kaynakId, hedefId, hedefAd);
            }
            case MASRAF -> masrafRepository.kategoriBirlestir(kaynakId, hedefId, hedefAd);
            case BUTCE -> butceRepository.kategoriBirlestir(kaynakId, hedefId, hedefAd);
            case KASA_GELIR, KASA_GIDER -> kasaHareketRepository.kategoriBirlestir(kaynakId, hedef);
        }

        kaynak.setUstKategori(null);
        kaynak.setAktif(false);
        kategoriRepository.save(kaynak);

        log.info("Kategori birleştirildi: {} -> {} (kapsam: {})",
                kaynak.getAd(), hedef.getAd(), hedef.getKapsam());
        return entityToDTO(hedef,
                kullanimHaritasi(hedef.getSirketId(), hedef.getKapsam()),
                altKategoriSayilari(hedef.getSirketId()));
    }

    /**
     * Get-or-create: domain servisleri (stok/masraf/bütçe/iskonto) legacy ad
     * alanından registry kaydını çözer. Boş ad {@code null} döner.
     *
     * <p>Var olan kayıt (pasif olsa bile) tekrar kullanılır; böylece geçmiş
     * kayıtların bağı kopmaz.
     */
    @CacheEvict(value = "lookup", allEntries = true)
    public Kategori resolve(Long sirketId, KategoriKapsam kapsam, String ad) {
        if (sirketId == null || kapsam == null) return null;
        String normal = normaliseAd(kapsam, ad);
        if (normal == null || normal.isBlank()) return null;
        Optional<Kategori> mevcut =
                kategoriRepository.findBySirketIdAndKapsamAndAdIgnoreCase(sirketId, kapsam, normal);
        if (mevcut.isPresent()) return mevcut.get();
        Kategori yeni = Kategori.builder()
                .sirketId(sirketId)
                .kapsam(kapsam)
                .ad(normal)
                .sira(0)
                .aktif(true)
                .build();
        return kategoriRepository.save(yeni);
    }

    // ---------------------------------------------------------------
    // Yardımcılar
    // ---------------------------------------------------------------

    public Kategori getEntity(Long id) {
        if (id == null) throw new ResourceNotFoundException("Kategori", null);
        Kategori k = kategoriRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori", id));
        tenantChecker.check(k.getSirketId(), "Kategori");
        return k;
    }

    private KategoriKapsam kapsamCoz(String kapsam, String tur) {
        if (kapsam != null && !kapsam.isBlank()) {
            try {
                return KategoriKapsam.valueOf(kapsam.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Geçersiz kategori kapsamı: " + kapsam);
            }
        }
        KategoriKapsam t = KategoriKapsam.turdan(tur);
        if (t == null) throw new BusinessException("Kategori kapsamı seçilmelidir");
        return t;
    }

    /** MASRAF/BUTCE adları büyük harfe normalize edilir; diğerleri yalnızca kırpılır. */
    private String normaliseAd(KategoriKapsam kapsam, String ad) {
        if (ad == null) return null;
        String temiz = ad.trim();
        if (temiz.isEmpty()) return null;
        return kapsam != null && kapsam.buyukHarf()
                ? temiz.toUpperCase(Locale.ROOT)
                : temiz;
    }

    private Kategori ustKategoriCoz(Long ustKategoriId, Long sirketId, KategoriKapsam kapsam, Long kendisiId) {
        if (ustKategoriId == null) return null;
        Kategori ust = kategoriRepository.findById(ustKategoriId)
                .orElseThrow(() -> new ResourceNotFoundException("Üst kategori", ustKategoriId));
        tenantChecker.check(ust.getSirketId(), "Üst kategori");
        if (!Objects.equals(ust.getSirketId(), sirketId)) {
            throw new BusinessException("Üst kategori aynı şirkete ait olmalıdır");
        }
        if (kendisiId != null && kendisiId.equals(ust.getId())) {
            throw new BusinessException("Kategori kendisine bağlanamaz");
        }
        if (kapsam != ust.getKapsam()) {
            throw new BusinessException("Üst kategori aynı kapsamda olmalıdır");
        }
        if (ust.getUstKategori() != null) {
            throw new BusinessException("Alt kategoriye alt kategori eklenemez (en fazla 2 seviye)");
        }
        return ust;
    }

    private void renkDogrula(String renk) {
        if (renk == null || renk.isBlank()) return;
        if (!RENK_PALETI.contains(renk.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException("Geçersiz renk; sabit paletten seçim yapın");
        }
    }

    private void ikonDogrula(String ikon) {
        if (ikon == null || ikon.isBlank()) return;
        if (!IKON_DESENI.matcher(ikon.trim()).matches()) {
            throw new BusinessException("Geçersiz ikon; 'pi pi-...' biçiminde olmalıdır");
        }
    }

    private void siraDogrula(Integer sira) {
        if (sira != null && sira < 0) {
            throw new BusinessException("Sıra negatif olamaz");
        }
    }

    private String bosIseNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Tek kategori kullanımı (silme/kapsam değişikliği öncesi). */
    private long kullanimSayisi(Kategori k) {
        Long id = k.getId();
        if (id == null) return 0;
        return switch (k.getKapsam()) {
            case KASA_GELIR, KASA_GIDER -> kasaHareketRepository.countByKategoriId(id);
            case STOK -> stokRepository.countByKategoriId(id) + iskontoKuraliRepository.countByKategoriId(id);
            case MASRAF -> masrafRepository.countByKategoriId(id);
            case BUTCE -> butceRepository.countByKategoriId(id);
        };
    }

    /** Kapsamdaki tüm kategoriler için kullanım sayıları (tek/az sorgu). */
    private Map<Long, Long> kullanimHaritasi(Long sirketId, KategoriKapsam kapsam) {
        Map<Long, Long> sonuc = new HashMap<>();
        if (sirketId == null) return sonuc;
        if (kapsam == null || kapsam == KategoriKapsam.KASA_GELIR || kapsam == KategoriKapsam.KASA_GIDER) {
            ekle(sonuc, kasaHareketRepository.kategoriKullanimSayilari(sirketId));
        }
        if (kapsam == null || kapsam == KategoriKapsam.STOK) {
            ekle(sonuc, stokRepository.kategoriKullanimSayilari(sirketId));
            ekle(sonuc, iskontoKuraliRepository.kategoriKullanimSayilari(sirketId));
        }
        if (kapsam == null || kapsam == KategoriKapsam.MASRAF) {
            ekle(sonuc, masrafRepository.kategoriKullanimSayilari(sirketId));
        }
        if (kapsam == null || kapsam == KategoriKapsam.BUTCE) {
            ekle(sonuc, butceRepository.kategoriKullanimSayilari(sirketId));
        }
        return sonuc;
    }

    private void ekle(Map<Long, Long> hedef, List<Object[]> satirlar) {
        if (satirlar == null) return;
        for (Object[] satir : satirlar) {
            if (satir == null || satir.length < 2 || satir[0] == null) continue;
            Long id = satir[0] instanceof Number n ? n.longValue() : null;
            long adet = satir[1] instanceof Number n ? n.longValue() : 0L;
            if (id != null) hedef.merge(id, adet, Long::sum);
        }
    }

    /** Şirketteki kategoriler için alt kategori sayıları (tek sorgu). */
    private Map<Long, Long> altKategoriSayilari(Long sirketId) {
        Map<Long, Long> sonuc = new HashMap<>();
        if (sirketId == null) return sonuc;
        ekle(sonuc, kategoriRepository.altKategoriSayilari(sirketId));
        return sonuc;
    }

    /** Yeniden adlandırmada legacy string kolonlar tek transaction'da güncellenir. */
    private void legacyAdYayilim(Kategori k) {
        Long id = k.getId();
        String ad = k.getAd();
        switch (k.getKapsam()) {
            case STOK -> {
                stokRepository.kategoriAdGuncelle(id, ad);
                iskontoKuraliRepository.kategoriAdGuncelle(id, ad);
            }
            case MASRAF -> masrafRepository.kategoriAdGuncelle(id, ad);
            case BUTCE -> butceRepository.kategoriAdGuncelle(id, ad);
            case KASA_GELIR, KASA_GIDER -> {
                // Kasa hareketleri ilişki üzerinden okur; legacy string kolon yok.
            }
        }
    }

    private List<Kategori> filtrele(List<Kategori> liste, String q, Boolean aktif) {
        String desen = q == null ? null : q.trim().toLowerCase(Locale.ROOT);
        boolean aramaVar = desen != null && !desen.isEmpty();
        return liste.stream()
                .filter(k -> aktif == null || Objects.equals(k.getAktif(), aktif))
                .filter(k -> !aramaVar || (k.getAd() != null
                        && k.getAd().toLowerCase(Locale.ROOT).contains(desen)))
                .toList();
    }

    private List<KategoriAgacDTO> agacKur(List<Kategori> dugumler, List<Kategori> tumu,
                                          Map<Long, Long> kullanim, boolean kati) {
        Map<Long, KategoriAgacDTO> harita = new LinkedHashMap<>();
        for (Kategori k : dugumler) {
            harita.put(k.getId(), agacDTO(k, kullanim.getOrDefault(k.getId(), 0L)));
        }
        Map<Long, Boolean> tumundekiIdler = new HashMap<>();
        for (Kategori k : tumu) {
            if (k.getId() != null) tumundekiIdler.put(k.getId(), true);
        }
        List<KategoriAgacDTO> kokler = new ArrayList<>();
        for (Kategori k : dugumler) {
            Long ustId = k.getUstKategori() != null ? k.getUstKategori().getId() : null;
            KategoriAgacDTO dugum = harita.get(k.getId());
            KategoriAgacDTO ust = ustId != null ? harita.get(ustId) : null;
            if (ust != null) {
                ust.getAltKategoriler().add(dugum);
            } else if (!kati && ustId != null && tumundekiIdler.containsKey(ustId)) {
                // Filtre nedeniyle üst kategori listede yok: düğüm köke taşınır ki
                // arama sonucu sessizce kaybolmasın.
                kokler.add(dugum);
            } else if (ustId == null) {
                kokler.add(dugum);
            }
            // katı modda (dropdown) üstü görünmeyen düğüm tamamen atlanır.
        }
        // Alt kategoriler sıra/ad ile deterministik döner.
        Comparator<KategoriAgacDTO> sirala = Comparator
                .comparing((KategoriAgacDTO d) -> d.getSira() != null ? d.getSira() : 0)
                .thenComparing((KategoriAgacDTO d) -> d.getAd() != null ? d.getAd() : "",
                        String.CASE_INSENSITIVE_ORDER);
        kokler.sort(sirala);
        for (KategoriAgacDTO d : harita.values()) {
            d.getAltKategoriler().sort(sirala);
            d.setAltKategoriSayisi((long) d.getAltKategoriler().size());
        }
        return kokler;
    }

    private KategoriAgacDTO agacDTO(Kategori k, long kullanim) {
        return KategoriAgacDTO.builder()
                .id(k.getId())
                .kapsam(k.getKapsam() != null ? k.getKapsam().name() : null)
                .ad(k.getAd())
                .ustKategoriId(k.getUstKategori() != null ? k.getUstKategori().getId() : null)
                .ustKategoriAd(k.getUstKategori() != null ? k.getUstKategori().getAd() : null)
                .renk(k.getRenk())
                .ikon(k.getIkon())
                .sira(k.getSira())
                .aktif(k.getAktif())
                .aciklama(k.getAciklama())
                .kullanimSayisi(kullanim)
                .altKategoriSayisi(0L)
                .olusturmaTarihi(k.getOlusturmaTarihi())
                .guncellemeTarihi(k.getGuncellemeTarihi())
                .tur(k.getKapsam() != null ? k.getKapsam().turKarsiligi() : null)
                .build();
    }

    private KategoriDTO entityToDTO(Kategori k, Map<Long, Long> kullanim, Map<Long, Long> altSayilari) {
        Long id = k.getId();
        long kullanimSayisi = id != null ? kullanim.getOrDefault(id, 0L) : 0L;
        long altKategori = id != null ? altSayilari.getOrDefault(id, 0L) : 0L;
        return KategoriDTO.builder()
                .id(id)
                .kapsam(k.getKapsam() != null ? k.getKapsam().name() : null)
                .ad(k.getAd())
                .ustKategoriId(k.getUstKategori() != null ? k.getUstKategori().getId() : null)
                .ustKategoriAd(k.getUstKategori() != null ? k.getUstKategori().getAd() : null)
                .renk(k.getRenk())
                .ikon(k.getIkon())
                .sira(k.getSira())
                .aktif(k.getAktif())
                .aciklama(k.getAciklama())
                .altKategoriSayisi(altKategori)
                .kullanimSayisi(kullanimSayisi)
                .olusturmaTarihi(k.getOlusturmaTarihi())
                .guncellemeTarihi(k.getGuncellemeTarihi())
                .tur(k.getKapsam() != null ? k.getKapsam().turKarsiligi() : null)
                .build();
    }
}
