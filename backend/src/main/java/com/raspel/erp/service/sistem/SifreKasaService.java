package com.raspel.erp.service.sistem;

import com.raspel.erp.config.TenantChecker;
import com.raspel.erp.dto.sistem.SifreKasaDTO;
import com.raspel.erp.dto.sistem.SifreKasaOzetDTO;
import com.raspel.erp.entity.sistem.SifreKasa;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.SifreKasaRepository;
import com.raspel.erp.util.AesGcmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Sifre kasasi is mantigi.
 *
 * <p><b>GUVENLIK KURALLARI (bu sinifin varlik nedeni):</b>
 * <ol>
 *   <li>Sifre metni duz metin SAKLANMAZ. Yazma yolunda AES-256-GCM ile
 *       sifrelenir ({@link AesGcmUtil}), okuma yolunda yalniz erisim kurali
 *       dogrulandiktan sonra cozulur.</li>
 *   <li>Liste ve ozet ciktilarinda sifre alani YOKTUR. Onaylanan bir
 *       "ac" ucu vardir ve her cagrisi denetim izine yazilir.</li>
 *   <li>Tenant izolasyonu sorgu seviyesinde uygulanir; ayrica servis
 *       katmaninda da dogrulanir.</li>
 *   <li>Yazma anahtari tanimli degilse sessizce gecici anahtar URETILMEZ
 *       (restart sonrasi tum sifreler okunmaz olurdu). Bunun yerine islem
 *       acik bir hatayla reddedilir.</li>
 * </ol>
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SifreKasaService {

    /** Sure uyarisi esigi: kalan gun 14 veya daha azsa uyarilir. (Sabit.) */
    public static final int UYARI_ESIGI_GUN = 14;

    private static final String KAPSAM_KISISEL = "KISISEL";
    private static final String KAPSAM_GLOBAL = "GLOBAL";
    private static final String GORUNURLUK_SAHIS = "SAHIS";
    private static final String GORUNURLUK_TUMU = "TUMU";

    private static final Set<String> KATEGORILER =
            Set.of("SISTEM", "BANKA", "EPOSTA", "SOSYAL", "DIGER");

    private static final String DURUM_SURESIZ = "SURESIZ";
    private static final String DURUM_GECERLI = "GECERLI";
    private static final String DURUM_SURE_YAKLASTI = "SURE_YAKLASTI";
    private static final String DURUM_SURESI_BITTI = "SURESI_BITTI";

    private final SifreKasaRepository sifreKasaRepository;
    private final TenantChecker tenantChecker;
    private final AuditLogService auditLogService;

    /** AES anahtari. Ortam degiskeninden okunur; prod'da zorunludur. */
    @Value("${app.vault.encryption-key:}")
    private String encryptionKey;

    // ------------------------------------------------------------------
    // Okuma
    // ------------------------------------------------------------------

    /**
     * Kasa listesi. KISISEL kapsamda yalnizca cagiranin kayitlari,
     * GLOBAL kapsamda sirketin butun kayitlari doner.
     */
    @Transactional(readOnly = true)
    public List<SifreKasaDTO> listele(Long sirketId, Long kullaniciId, String kapsam,
                                     Boolean aktif, String kategori, String q, boolean admin) {
        String temizKapsam = normaliseKapsam(kapsam);
        String temizKategori = bosTemizle(kategori) == null ? null
                : kategoriDogrula(kategori);
        String temizArama = bosTemizle(q);

        List<SifreKasa> kayitlar;
        if (KAPSAM_KISISEL.equals(temizKapsam)) {
            // Kullanicinin KISISEL kayitlari. Diger kullanicilarin kisisel
            // kasasi hicbir kosulda listelenmez.
            kayitlar = sifreKasaRepository.kisiselKayitlar(sirketId, kullaniciId, aktif);
        } else if (KAPSAM_GLOBAL.equals(temizKapsam)) {
            kayitlar = sifreKasaRepository.filtreli(sirketId, KAPSAM_GLOBAL, aktif, temizKategori, temizArama);
        } else {
            // Kapsam verilmediyse: kullanicinin kisisel + sirketin global kayitlari.
            kayitlar = new java.util.ArrayList<>(
                    sifreKasaRepository.kisiselKayitlar(sirketId, kullaniciId, aktif));
            kayitlar.addAll(sifreKasaRepository.filtreli(sirketId, KAPSAM_GLOBAL, aktif, temizKategori, temizArama));
        }

        return kayitlar.stream()
                .filter(k -> temizKategori == null || temizKategori.equalsIgnoreCase(k.getKategori()))
                .filter(k -> temizArama == null || aramaEslesiyor(k, temizArama))
                .map(k -> entityToDTO(k, false))
                .toList();
    }

    /** Bir kaydin meta bilgisini doner. Sifre alani YOKTUR. */
    @Transactional(readOnly = true)
    public SifreKasaDTO idyeGoreGetir(Long id, Long sirketId, Long kullaniciId) {
        SifreKasa kayit = bulVeTenantKontrol(id, sirketId);
        return entityToDTO(kayit, false);
    }

    /**
     * Sifreyi duz metin olarak acar. Kural:
     * <ul>
     *   <li>GLOBAL + TUMU -> herkes acabilir</li>
     *   <li>diger her durumda -> kaydi tutan kullanici veya ADMIN</li>
     * </ul>
     * Her cagri denetim izine "SIFRE_GORUNTULENME" olarak yazilir.
     */
    @Transactional
    public String sifreyiGoster(Long id, Long sirketId, Long kullaniciId, boolean admin, String ipAdresi) {
        SifreKasa kayit = bulVeTenantKontrol(id, sirketId);

        if (!sifreGorunur(kayit, kullaniciId, admin)) {
            // Varlik sizdirmasin: yetki yoksa "bulunamadi" don.
            throw new ResourceNotFoundException("Sifre kasasi kaydi bulunamadi: " + id);
        }

        String duz = coz(kayit.getSifreCipher());

        kayit.setSonGoruntuleme(LocalDateTime.now());
        sifreKasaRepository.save(kayit);

        auditLogService.log(kullaniciId, sirketId, "SIFRE_GORUNTULENME", "SifreKasa", kayit.getId(),
                "Sifre kasasi kaydi acildi", ipAdresi,
                "{\"baslik\":\"" + temizle(kayit.getBaslik()) + "\",\"kapsam\":\"" + kayit.getKapsam()
                        + "\",\"gorunurluk\":\"" + kayit.getSifreGorunurlugu() + "\"}");

        return duz;
    }

    /** Sidebar rozeti ve sayfa ust kismi icin ozet sayaclar. */
    @Transactional(readOnly = true)
    public SifreKasaOzetDTO ozet(Long sirketId, Long kullaniciId) {
        LocalDateTime simdi = LocalDateTime.now();

        long suresiBitti = sifreKasaRepository.countSuresiBitti(sirketId, simdi);
        long uyari = sifreKasaRepository.countUyari(sirketId,
                simdi.minusDays(UYARI_ESIGI_GUN));

        return SifreKasaOzetDTO.builder()
                .uyari(uyari)
                .suresiBitti(suresiBitti)
                .sirketGeneli(sifreKasaRepository.countBySirketIdAndAktifTrueAndKapsamAndSifreGorunurlugu(
                        sirketId, KAPSAM_GLOBAL, GORUNURLUK_TUMU))
                .arsiv(sifreKasaRepository.countBySirketIdAndAktifFalse(sirketId))
                .build();
    }

    // ------------------------------------------------------------------
    // Yazma
    // ------------------------------------------------------------------

    @Transactional
    public SifreKasaDTO olustur(SifreKasaDTO dto, Long sirketId, Long kullaniciId, boolean admin) {
        if (sirketId == null) {
            throw new BusinessException("Şirket bilgisi bulunamadı");
        }

        String kapsam = normaliseKapsam(dto.getKapsam());
        String gorunurluk = normaliseGorunurlugu(dto.getSifreGorunurlugu());

        // Global kasayi yalniz ADMIN yazabilir.
        if (KAPSAM_GLOBAL.equals(kapsam) && !admin) {
            throw new BusinessException("Şirkete ait (global) kasaya yalnızca yönetici ekleyebilir");
        }
        gorunurlukDogrula(kapsam, gorunurluk);

        if (dto.getBaslik() == null || dto.getBaslik().trim().isEmpty()) {
            throw new BusinessException("Başlık boş olamaz");
        }
        if (dto.getSifre() == null || dto.getSifre().isEmpty()) {
            throw new BusinessException("Şifre boş olamaz");
        }
        if (dto.getBaslik().trim().length() > 200) {
            throw new BusinessException("Başlık en fazla 200 karakter olabilir");
        }

        String baslik = dto.getBaslik().trim();
        if (sifreKasaRepository.existsBySirketIdAndBaslikIgnoreCaseAndAktifTrue(sirketId, baslik)) {
            throw new BusinessException("Bu başlıkta bir kayıt zaten var: " + baslik);
        }

        LocalDateTime simdi = LocalDateTime.now();
        SifreKasa kayit = SifreKasa.builder()
                .sirketId(sirketId)
                // Global kayitlarda sahip yoktur: yalniz ADMIN yazabilir.
                .kullaniciId(KAPSAM_GLOBAL.equals(kapsam) ? null : kullaniciId)
                .kapsam(kapsam)
                .sifreGorunurlugu(gorunurluk)
                .baslik(baslik)
                .kullaniciAdi(bosTemizle(dto.getKullaniciAdi()))
                .sifreCipher(sifrele(dto.getSifre()))
                .url(bosTemizle(dto.getUrl()))
                .kategori(kategoriDogrula(dto.getKategori()))
                .notlar(bosTemizle(dto.getNotlar()))
                .gecerlilikGun(gecerlilikDogrula(dto.getGecerlilikGun()))
                .sifreDegisimTarihi(simdi)
                .aktif(true)
                .olusturmaTarihi(simdi)
                .build();

        return entityToDTO(sifreKasaRepository.save(kayit), false);
    }

    @Transactional
    public SifreKasaDTO guncelle(Long id, SifreKasaDTO dto, Long sirketId,
                                 Long kullaniciId, boolean admin) {
        SifreKasa kayit = bulVeTenantKontrol(id, sirketId);
        sahiplikKontrol(kayit, kullaniciId, admin);

        if (KAPSAM_GLOBAL.equals(kayit.getKapsam()) && !admin) {
            throw new BusinessException("Şirkete ait (global) kayıtları yalnızca yönetici düzenleyebilir");
        }

        if (dto.getBaslik() != null && !dto.getBaslik().trim().isEmpty()) {
            String baslik = dto.getBaslik().trim();
            if (baslik.length() > 200) {
                throw new BusinessException("Başlık en fazla 200 karakter olabilir");
            }
            if (!baslik.equalsIgnoreCase(kayit.getBaslik())
                    && sifreKasaRepository.existsBySirketIdAndBaslikIgnoreCaseAndAktifTrueAndIdNot(
                    sirketId, baslik, id)) {
                throw new BusinessException("Bu başlıkta bir kayıt zaten var: " + baslik);
            }
            kayit.setBaslik(baslik);
        }

        if (dto.getKullaniciAdi() != null) kayit.setKullaniciAdi(bosTemizle(dto.getKullaniciAdi()));
        if (dto.getUrl() != null) kayit.setUrl(bosTemizle(dto.getUrl()));
        if (dto.getNotlar() != null) kayit.setNotlar(bosTemizle(dto.getNotlar()));
        if (dto.getKategori() != null) kayit.setKategori(kategoriDogrula(dto.getKategori()));
        if (dto.getGecerlilikGun() != null) kayit.setGecerlilikGun(gecerlilikDogrula(dto.getGecerlilikGun()));

        // ONEYLI TASARIM: bos sifre "degistirme" demektir, mevcut korunur.
        // Sifre metni ANCAK gercekten degisirse sure referansi sifirlanir;
        // baslik/not duzenlemesi surEYI uzatmaz.
        if (dto.getSifre() != null && !dto.getSifre().isEmpty()) {
            if (dto.getSifre().length() > 500) {
                throw new BusinessException("Şifre en fazla 500 karakter olabilir");
            }
            kayit.setSifreCipher(sifrele(dto.getSifre()));
            kayit.setSifreDegisimTarihi(LocalDateTime.now());
            kayit.setSonGoruntuleme(null);
        }

        return entityToDTO(sifreKasaRepository.save(kayit), false);
    }

    /**
     * Arsivler (soft delete). Fiziksel silme YOKTUR: kayit aktif=false
     * olur ve geri alinabilir. Yanlislikla arsivlenen kayit kalici olarak
     * kaybolmamalidir.
     */
    @Transactional
    public void arsivle(Long id, Long sirketId, Long kullaniciId, boolean admin) {
        SifreKasa kayit = bulVeTenantKontrol(id, sirketId);
        sahiplikKontrol(kayit, kullaniciId, admin);
        if (Boolean.FALSE.equals(kayit.getAktif())) return; // idempotent

        kayit.setAktif(false);
        kayit.setArsivTarihi(LocalDateTime.now());
        sifreKasaRepository.save(kayit);
    }

    /** Arsivden cikarir. */
    @Transactional
    public void arsivleGeriAl(Long id, Long sirketId, Long kullaniciId, boolean admin) {
        SifreKasa kayit = bulVeTenantKontrol(id, sirketId);
        sahiplikKontrol(kayit, kullaniciId, admin);
        if (Boolean.TRUE.equals(kayit.getAktif())) return; // idempotent

        kayit.setAktif(true);
        kayit.setArsivTarihi(null);
        sifreKasaRepository.save(kayit);
    }

    // ------------------------------------------------------------------
    // Erisim kurallari
    // ------------------------------------------------------------------

    /**
     * Sifre metnini kim acabilir?
     * <ul>
     *   <li>GLOBAL + TUMU -> herkes (sirket geneli erisim)</li>
     *   <li>diger her durumda -> kaydi tutan kullanici veya ADMIN</li>
     * </ul>
     */
    private boolean sifreGorunur(SifreKasa kayit, Long kullaniciId, boolean admin) {
        if (kayit.globalMi() && GORUNURLUK_TUMU.equals(kayit.getSifreGorunurlugu())) {
            return true;
        }
        return admin || Objects.equals(kayit.getKullaniciId(), kullaniciId);
    }

    /** Kaydi duzenleme/arsivleme/silme hakki: sahibi veya ADMIN. */
    private void sahiplikKontrol(SifreKasa kayit, Long kullaniciId, boolean admin) {
        if (admin) return;
        if (kayit.globalMi()) {
            throw new BusinessException("Şirkete ait (global) kayıtları yalnızca yönetici işleyebilir");
        }
        if (kayit.getKullaniciId() == null || !Objects.equals(kayit.getKullaniciId(), kullaniciId)) {
            throw new ResourceNotFoundException("Sifre kasasi kaydi bulunamadi: " + kayit.getId());
        }
    }

    private SifreKasa bulVeTenantKontrol(Long id, Long sirketId) {
        SifreKasa kayit = sifreKasaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sifre kasasi kaydi bulunamadi: " + id));
        tenantChecker.check(kayit.getSirketId(), "SifreKasa");
        return kayit;
    }

    // ------------------------------------------------------------------
    // Sifreleme
    // ------------------------------------------------------------------

    /** Sifre metnini AES-256-GCM ile sifreler, Base64 dondurur. */
    private String sifrele(String duz) {
        String anahtar = anahtarZorunlu();
        byte[] ivVeSifreli = AesGcmUtil.encrypt(duz.getBytes(StandardCharsets.UTF_8), anahtar);
        return Base64.getEncoder().encodeToString(ivVeSifreli);
    }

    private String coz(String base64) {
        String anahtar = anahtarZorunlu();
        try {
            byte[] ivVeSifreli = Base64.getDecoder().decode(base64);
            return new String(AesGcmUtil.decrypt(ivVeSifreli, anahtar), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Şifre kaydı okunamadı (anahtar değişmiş olabilir)");
        } catch (RuntimeException e) {
            throw new BusinessException("Şifre kaydı çözülemedi (anahtar değişmiş olabilir)", e);
        }
    }

    /**
     * Anahtar zorunludur. AiConfigService'in yaptigi gibi sessizce gecici
     * anahtar uretilmez: restart sonrasi tum sifreler okunmaz hale gelirdi.
     */
    private String anahtarZorunlu() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new BusinessException(
                    "Şifre kasası şifreleme anahtarı tanımlı değil (APP_VAULT_ENCRYPTION_KEY). "
                            + "Kasa kullanılamıyor.");
        }
        return encryptionKey;
    }

    /** Test/entegrasyon icin anahtar set edici (Spring disi kurulumlar icin). */
    void anahtarAyar(String anahtar) {
        this.encryptionKey = anahtar;
    }

    // ------------------------------------------------------------------
    // Dogrulama ve normalizasyon
    // ------------------------------------------------------------------

    private void gorunurlukDogrula(String kapsam, String gorunurluk) {
        if (GORUNURLUK_TUMU.equals(gorunurluk) && !KAPSAM_GLOBAL.equals(kapsam)) {
            throw new BusinessException(
                    "Şirkette herkese açık şifre yalnızca global kasada tanımlanabilir");
        }
    }

    private String normaliseKapsam(String kapsam) {
        if (kapsam == null || kapsam.trim().isEmpty()) return KAPSAM_KISISEL;
        String k = kapsam.trim().toUpperCase();
        if (!KAPSAM_KISISEL.equals(k) && !KAPSAM_GLOBAL.equals(k)) {
            throw new BusinessException("Geçersiz kapsam: " + kapsam);
        }
        return k;
    }

    private String normaliseGorunurlugu(String gorunurluk) {
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

    private Integer gecerlilikDogrula(Integer gun) {
        if (gun == null || gun == 0) return null;
        if (gun < 0) {
            throw new BusinessException("Geçerlilik süresi negatif olamaz");
        }
        if (gun > 3650) {
            throw new BusinessException("Geçerlilik süresi en fazla 3650 gün olabilir");
        }
        return gun;
    }

    private String bosTemizle(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean aramaEslesiyor(SifreKasa k, String q) {
        String aranan = q.toLowerCase();
        return k.getBaslik().toLowerCase().contains(aranan)
                || (k.getKullaniciAdi() != null && k.getKullaniciAdi().toLowerCase().contains(aranan))
                || (k.getUrl() != null && k.getUrl().toLowerCase().contains(aranan));
    }

    private String temizle(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }

    // ------------------------------------------------------------------
    // Sure hesabi ve esleme
    // ------------------------------------------------------------------

    /** Suresi dolmus / yaklasiyor / gecerli / suresiz. */
    static String sureDurumu(SifreKasa kayit, LocalDateTime simdi) {
        if (kayit.getGecerlilikGun() == null || kayit.getGecerlilikGun() <= 0) {
            return DURUM_SURESIZ;
        }
        if (kayit.getSifreDegisimTarihi() == null) {
            return DURUM_SURESIZ;
        }
        long gecenGun = Duration.between(kayit.getSifreDegisimTarihi(), simdi).toDays();
        long kalan = kayit.getGecerlilikGun() - gecenGun;
        if (kalan <= 0) return DURUM_SURESI_BITTI;
        if (kalan <= UYARI_ESIGI_GUN) return DURUM_SURE_YAKLASTI;
        return DURUM_GECERLI;
    }

    static Integer kalanGun(SifreKasa kayit, LocalDateTime simdi) {
        if (kayit.getGecerlilikGun() == null || kayit.getGecerlilikGun() <= 0
                || kayit.getSifreDegisimTarihi() == null) {
            return null;
        }
        long gecenGun = Duration.between(kayit.getSifreDegisimTarihi(), simdi).toDays();
        return (int) (kayit.getGecerlilikGun() - gecenGun);
    }

    /**
     * Entity -> DTO. {@code sifreAlan Dahil} false iken sifre hicbir sekilde
     * ciktiya girmez; liste/ozet ucu yalnizca bu yontemi kullanir.
     */
    private SifreKasaDTO entityToDTO(SifreKasa kayit, boolean sifreAlanDahil) {
        LocalDateTime simdi = LocalDateTime.now();
        return SifreKasaDTO.builder()
                .id(kayit.getId())
                .baslik(kayit.getBaslik())
                .kullaniciAdi(kayit.getKullaniciAdi())
                .url(kayit.getUrl())
                .kategori(kayit.getKategori())
                .notlar(kayit.getNotlar())
                .kapsam(kayit.getKapsam())
                .sifreGorunurlugu(kayit.getSifreGorunurlugu())
                .gecerlilikGun(kayit.getGecerlilikGun())
                .durum(sureDurumu(kayit, simdi))
                .kalanGun(kalanGun(kayit, simdi))
                .arsiv(!Boolean.TRUE.equals(kayit.getAktif()))
                .olusturmaTarihi(kayit.getOlusturmaTarihi())
                .guncellemeTarihi(kayit.getGuncellemeTarihi())
                .sonGoruntuleme(kayit.getSonGoruntuleme())
                .build();
    }
}