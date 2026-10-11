package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.KitaplikDTO;
import com.raspel.erp.entity.sistem.Kitaplik;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.KitaplikRepository;
import com.raspel.erp.util.AramaTemizleyici;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Kitaplik (yer imleri + snippet) is mantigi.
 *
 * <p>Sir icermez; sifre kasasindan farkli olarak sifreleme/acma-denetimi yoktur.
 * Kapsam/gorunurluk sozlesmesi kasayla aynidir:
 * <ul>
 *   <li>KISISEL: yalniz sahibi (ve ADMIN) gorur/duzenler.</li>
 *   <li>GLOBAL + SAHIS: yalniz ADMIN gorur.</li>
 *   <li>GLOBAL + TUMU: sirketteki herkes gorur; duzenleme yalniz ADMIN.</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class KitaplikService {

    private static final String TUR_YER_IMI = "YER_IMI";
    private static final String TUR_SNIPPET = "SNIPPET";
    private static final Set<String> TURLER = Set.of(TUR_YER_IMI, TUR_SNIPPET);

    private static final String KAPSAM_KISISEL = "KISISEL";
    private static final String KAPSAM_GLOBAL = "GLOBAL";

    private static final String GORUNURLUK_SAHIS = "SAHIS";
    private static final String GORUNURLUK_TUMU = "TUMU";

    private static final Set<String> KATEGORILER =
            Set.of("SISTEM", "ARAC", "DOKUMAN", "KOD", "DIGER");

    private final KitaplikRepository kitaplikRepository;
    private final TenantChecker tenantChecker;

    @Transactional(readOnly = true)
    public List<KitaplikDTO> listele(Long sirketId, Long kullaniciId, boolean admin,
                                     String tur, Boolean aktif, String kategori, String q) {
        String temizTur = turDogrula(tur, true);
        String temizKategori = bosTemizle(kategori) == null ? null : kategoriDogrula(kategori);
        String desen = AramaTemizleyici.like(q);
        return kitaplikRepository
                .gorunurKayitlar(sirketId, kullaniciId, admin, temizTur, aktif, temizKategori, desen)
                .stream().map(this::entityToDTO).toList();
    }

    @Transactional(readOnly = true)
    public KitaplikDTO idyeGoreGetir(Long id, Long sirketId, Long kullaniciId, boolean admin) {
        Kitaplik kayit = bulVeTenantKontrol(id, sirketId);
        gorunurlukKontrol(kayit, kullaniciId, admin);
        return entityToDTO(kayit);
    }

    @Transactional
    public KitaplikDTO olustur(KitaplikDTO dto, Long sirketId, Long kullaniciId, boolean admin) {
        if (sirketId == null) {
            throw new BusinessException("Şirket bilgisi bulunamadı");
        }
        String tur = turDogrula(dto.getTur(), false);
        String kapsam = normaliseKapsam(dto.getKapsam());
        String gorunurluk = normaliseGorunurluk(dto.getGorunurluk());

        if (KAPSAM_GLOBAL.equals(kapsam) && !admin) {
            throw new BusinessException("Şirkete ait (global) kitaplığa yalnızca yönetici ekleyebilir");
        }
        gorunurlukDogrula(kapsam, gorunurluk);
        icerikDogrula(tur, dto);
        baslikDogrula(dto.getBaslik());

        String baslik = dto.getBaslik().trim();
        // Ayni sahiplik/kapsam icinde ayni tur+baslikta aktif kayit olmasin.
        boolean var = KAPSAM_GLOBAL.equals(kapsam)
                ? kitaplikRepository.existsBySirketIdAndKapsamAndTurAndBaslikIgnoreCaseAndAktifTrue(
                        sirketId, KAPSAM_GLOBAL, tur, baslik)
                : kitaplikRepository.existsBySirketIdAndKullaniciIdAndTurAndBaslikIgnoreCaseAndAktifTrue(
                        sirketId, kullaniciId, tur, baslik);
        if (var) {
            throw new BusinessException("Bu başlıkta bir kayıt zaten var: " + baslik);
        }

        Kitaplik kayit = Kitaplik.builder()
                .sirketId(sirketId)
                .kullaniciId(KAPSAM_GLOBAL.equals(kapsam) ? null : kullaniciId)
                .tur(tur)
                .kapsam(kapsam)
                .gorunurluk(gorunurluk)
                .baslik(baslik)
                .url(bosTemizle(dto.getUrl()))
                .icerik(bosTemizle(dto.getIcerik()))
                .kategori(dto.getKategori() == null ? null : kategoriDogrula(dto.getKategori()))
                .notlar(bosTemizle(dto.getNotlar()))
                .aktif(true)
                .olusturmaTarihi(LocalDateTime.now())
                .build();

        return entityToDTO(kitaplikRepository.save(kayit));
    }

    @Transactional
    public KitaplikDTO guncelle(Long id, KitaplikDTO dto, Long sirketId,
                                Long kullaniciId, boolean admin) {
        Kitaplik kayit = bulVeTenantKontrol(id, sirketId);
        duzenlemeYetkisi(kayit, kullaniciId, admin);

        if (dto.getBaslik() != null && !dto.getBaslik().trim().isEmpty()) {
            baslikDogrula(dto.getBaslik());
            kayit.setBaslik(dto.getBaslik().trim());
        }
        if (dto.getUrl() != null) kayit.setUrl(bosTemizle(dto.getUrl()));
        if (dto.getIcerik() != null) kayit.setIcerik(bosTemizle(dto.getIcerik()));
        if (dto.getNotlar() != null) kayit.setNotlar(bosTemizle(dto.getNotlar()));
        if (dto.getKategori() != null) kayit.setKategori(kategoriDogrula(dto.getKategori()));

        // Guncelleme sonrasi icerik kurali hala saglanmali (YER_IMI icin url,
        // SNIPPET icin icerik).
        icerikDogrula(kayit.getTur(), dtoBirlestir(kayit));

        return entityToDTO(kitaplikRepository.save(kayit));
    }

    @Transactional
    public void arsivle(Long id, Long sirketId, Long kullaniciId, boolean admin) {
        Kitaplik kayit = bulVeTenantKontrol(id, sirketId);
        duzenlemeYetkisi(kayit, kullaniciId, admin);
        if (Boolean.FALSE.equals(kayit.getAktif())) return;
        kayit.setAktif(false);
        kayit.setArsivTarihi(LocalDateTime.now());
        kitaplikRepository.save(kayit);
    }

    @Transactional
    public void arsivleGeriAl(Long id, Long sirketId, Long kullaniciId, boolean admin) {
        Kitaplik kayit = bulVeTenantKontrol(id, sirketId);
        duzenlemeYetkisi(kayit, kullaniciId, admin);
        if (Boolean.TRUE.equals(kayit.getAktif())) return;
        kayit.setAktif(true);
        kayit.setArsivTarihi(null);
        kitaplikRepository.save(kayit);
    }

    // ------------------------------------------------------------------
    // Yetki ve dogrulama
    // ------------------------------------------------------------------

    /**
     * Okuma gorunurlugu (LISTE sorgusuyla TUTARLI):
     *   - GLOBAL + TUMU  -> herkes
     *   - GLOBAL + SAHIS -> yalniz ADMIN
     *   - KISISEL        -> yalniz sahibi (ADMIN dahi baskasinin kisisel kaydini gorMEZ)
     */
    private void gorunurlukKontrol(Kitaplik kayit, Long kullaniciId, boolean admin) {
        if (kayit.globalMi()) {
            if (GORUNURLUK_TUMU.equals(kayit.getGorunurluk())) return;
            if (admin) return;
            throw new ResourceNotFoundException("Kitaplik kaydi bulunamadi: " + kayit.getId());
        }
        if (!Objects.equals(kayit.getKullaniciId(), kullaniciId)) {
            throw new ResourceNotFoundException("Kitaplik kaydi bulunamadi: " + kayit.getId());
        }
    }

    /** Duzenleme/arsivleme: KISISEL -> sahibi/ADMIN; GLOBAL -> yalniz ADMIN. */
    private void duzenlemeYetkisi(Kitaplik kayit, Long kullaniciId, boolean admin) {
        if (admin) return;
        if (kayit.globalMi()) {
            throw new BusinessException("Şirkete ait (global) kayıtları yalnızca yönetici işleyebilir");
        }
        if (!Objects.equals(kayit.getKullaniciId(), kullaniciId)) {
            throw new ResourceNotFoundException("Kitaplik kaydi bulunamadi: " + kayit.getId());
        }
    }

    private Kitaplik bulVeTenantKontrol(Long id, Long sirketId) {
        Kitaplik kayit = kitaplikRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kitaplik kaydi bulunamadi: " + id));
        tenantChecker.check(kayit.getSirketId(), "Kitaplik");
        return kayit;
    }

    private void gorunurlukDogrula(String kapsam, String gorunurluk) {
        if (GORUNURLUK_TUMU.equals(gorunurluk) && !KAPSAM_GLOBAL.equals(kapsam)) {
            throw new BusinessException(
                    "Şirkette herkese açık kayıt yalnızca global kapsamda tanımlanabilir");
        }
    }

    private void icerikDogrula(String tur, KitaplikDTO dto) {
        if (TUR_YER_IMI.equals(tur)) {
            if (dto.getUrl() == null || dto.getUrl().trim().isEmpty()) {
                throw new BusinessException("Yer imi için adres (URL) zorunludur");
            }
        } else if (TUR_SNIPPET.equals(tur)) {
            if (dto.getIcerik() == null || dto.getIcerik().trim().isEmpty()) {
                throw new BusinessException("Snippet için içerik zorunludur");
            }
        }
    }

    private void baslikDogrula(String baslik) {
        if (baslik == null || baslik.trim().isEmpty()) {
            throw new BusinessException("Başlık boş olamaz");
        }
        if (baslik.trim().length() > 200) {
            throw new BusinessException("Başlık en fazla 200 karakter olabilir");
        }
    }

    private String turDogrula(String tur, boolean nullOlabilir) {
        if (tur == null || tur.trim().isEmpty()) {
            if (nullOlabilir) return null;
            throw new BusinessException("Tür zorunludur (YER_IMI veya SNIPPET)");
        }
        String t = tur.trim().toUpperCase();
        if (!TURLER.contains(t)) {
            throw new BusinessException("Geçersiz tür: " + tur);
        }
        return t;
    }

    private String normaliseKapsam(String kapsam) {
        if (kapsam == null || kapsam.trim().isEmpty()) return KAPSAM_KISISEL;
        String k = kapsam.trim().toUpperCase();
        if (!KAPSAM_KISISEL.equals(k) && !KAPSAM_GLOBAL.equals(k)) {
            throw new BusinessException("Geçersiz kapsam: " + kapsam);
        }
        return k;
    }

    private String normaliseGorunurluk(String gorunurluk) {
        if (gorunurluk == null || gorunurluk.trim().isEmpty()) return GORUNURLUK_SAHIS;
        String g = gorunurluk.trim().toUpperCase();
        if (!GORUNURLUK_SAHIS.equals(g) && !GORUNURLUK_TUMU.equals(g)) {
            throw new BusinessException("Geçersiz görünürlük: " + gorunurluk);
        }
        return g;
    }

    private String kategoriDogrula(String kategori) {
        if (kategori == null || kategori.trim().isEmpty()) return null;
        String k = kategori.trim().toUpperCase();
        if (!KATEGORILER.contains(k)) {
            throw new BusinessException("Geçersiz kategori: " + kategori);
        }
        return k;
    }

    private String bosTemizle(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Guncellemede icerik kuralini mevcut kayit degerleriyle tekrar dogrulamak icin. */
    private KitaplikDTO dtoBirlestir(Kitaplik k) {
        return KitaplikDTO.builder()
                .url(k.getUrl())
                .icerik(k.getIcerik())
                .build();
    }

    private KitaplikDTO entityToDTO(Kitaplik k) {
        return KitaplikDTO.builder()
                .id(k.getId())
                .tur(k.getTur())
                .baslik(k.getBaslik())
                .url(k.getUrl())
                .icerik(k.getIcerik())
                .kategori(k.getKategori())
                .notlar(k.getNotlar())
                .kapsam(k.getKapsam())
                .gorunurluk(k.getGorunurluk())
                .arsiv(!Boolean.TRUE.equals(k.getAktif()))
                .olusturmaTarihi(k.getOlusturmaTarihi())
                .guncellemeTarihi(k.getGuncellemeTarihi())
                .build();
    }
}