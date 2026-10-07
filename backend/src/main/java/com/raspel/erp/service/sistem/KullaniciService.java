package com.raspel.erp.service.sistem;

import com.raspel.erp.config.security.JwtUtil;
import com.raspel.erp.dto.sistem.KullaniciDTO;
import com.raspel.erp.dto.sistem.LoginRequest;
import com.raspel.erp.dto.sistem.LoginResponse;
import com.raspel.erp.dto.sistem.SifreDegistirRequest;
import com.raspel.erp.dto.sistem.TwoFactorGirisRequest;
import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.exception.BusinessException;
import com.raspel.erp.exception.DuplicateResourceException;
import com.raspel.erp.exception.ResourceNotFoundException;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.util.TotpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.time.Duration;
import com.raspel.erp.entity.sistem.Rol;
import com.raspel.erp.dto.sistem.SifreSifirlaRequest;
import com.raspel.erp.dto.sistem.TwoFactorDTO;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KullaniciService {

    private final KullaniciRepository kullaniciRepository;
    private final SirketRepository sirketRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final AktifOturumService aktifOturumService;
    private final com.raspel.erp.config.TenantChecker tenantChecker;
    private final com.raspel.erp.repository.sistem.SifreSifirlaTokenRepository sifreSifirlaTokenRepository;
    private final EmailService emailService;
    private final com.raspel.erp.repository.ik.PersonelRepository personelRepository;
    private final com.raspel.erp.service.sistem.AuditLogService auditLogService;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    /**
     * REDTEAM H-1: Platform geneli işlemlere (yedekleme/geri yükleme) yetkili
     * kullanıcı adları. {@code app.platform.admin-users} (ortam değişkeni
     * {@code APP_PLATFORM_ADMIN_USERS}) ile verilir; virgülle ayrılır.
     * Varsayılan {@code admin}. Boş bırakılırsa platform işlemleri KAPALI olur
     * (fail-closed): yedekleme gibi kritik işlemler sessizce açık kalmamalıdır.
     */
    @org.springframework.beans.factory.annotation.Value(
            "${app.platform.admin-users:${APP_PLATFORM_ADMIN_USERS:admin}}")
    private String platformAdminlarBoru = "admin";

    /** Platform yönetici beyaz listesi (karşılaştırma için küçük harfe indirgenmiş). */
    private java.util.Set<String> platformAdminlar() {
        if (platformAdminlarBoru == null || platformAdminlarBoru.isBlank()) {
            return java.util.Set.of();
        }
        return java.util.Arrays.stream(platformAdminlarBoru.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * Kimlik dogrulama denemeleri sayaci. Basarisiz girisler {sonuc} etiketiyle
     * "hatali_sifre" / "kullanici_yok" / "pasif" olarak sayilir; kaba kuvvet
     * (brute force) tespiti icin gereklidir (config/prometheus/alert.rules.yml:
     * LoginFailuresSpike). Kullanici adi etiketi EKLENMEZ: etiket olarak kullanici
     * adi koymak hem yuksek kardinalite hem de PII sizinti riski tasir.
     */
    private static final String METRIK_GIRIS = "raspel.giris.deneme";

    private void girisSayaci(String sonuc) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry == null) return;
        registry.counter(METRIK_GIRIS, "sonuc", sonuc).increment();
    }

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private static final long SIFRE_TOKEN_GECERLILIK_MS = 60 * 60 * 1000; // 1 saat

    /** Bekleyen girişler: girisToken -> (kullaniciId, oluşturmaZamani, 2FA dogrulandiMi). 5 dakika geçerli.
     *  Redis'te saklanır (sunucu restart'ında kaybolmaz); Redis'e erişilemezse bellek fallback'i kullanılır. */
    private final ConcurrentMap<String, long[]> bekleyenGirislerBellek = new ConcurrentHashMap<>();
    private static final long GIRIS_TOKEN_GECERLILIK_MS = 5 * 60 * 1000;
    private static final String GIRIS_REDIS_PREFIX = "giris:bekleyen:";

    private void bekleyenKaydet(String token, long kullaniciId, long zaman) {
        bekleyenKaydet(token, kullaniciId, zaman, true);
    }

    private void bekleyenKaydet(String token, long kullaniciId, long zaman, boolean ikiFaktoriDogrulandi) {
        long dogrulandi = ikiFaktoriDogrulandi ? 1L : 0L;
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(GIRIS_REDIS_PREFIX + token,
                        kullaniciId + ":" + zaman + ":" + dogrulandi, Duration.ofMillis(GIRIS_TOKEN_GECERLILIK_MS));
                return;
            }
        } catch (Exception e) {
            log.warn("Redis erişilemedi, giriş oturumu bellekte tutulacak: {}", e.getMessage());
        }
        bekleyenGirislerBellek.put(token, new long[]{kullaniciId, zaman, dogrulandi});
    }

    private long[] bekleyenGetir(String token) {
        try {
            if (redisTemplate != null) {
                String val = redisTemplate.opsForValue().get(GIRIS_REDIS_PREFIX + token);
                if (val != null) {
                    String[] parcalar = val.split(":");
                    long dogrulandi = parcalar.length > 2 ? Long.parseLong(parcalar[2]) : 0L;
                    return new long[]{Long.parseLong(parcalar[0]), Long.parseLong(parcalar[1]), dogrulandi};
                }
            }
        } catch (Exception e) {
            log.warn("Redis erişilemedi, bellek fallback kullanılıyor: {}", e.getMessage());
        }
        return bekleyenGirislerBellek.get(token);
    }

    private void bekleyenSil(String token) {
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(GIRIS_REDIS_PREFIX + token);
            }
        } catch (Exception e) {
            // Redis'e erişilemezse bellek kaydı aşağıda siliniyor
        }
        bekleyenGirislerBellek.remove(token);
    }

    public Page<KullaniciDTO> tumunuGetir(Long sirketId, Pageable pageable) {
        if (sirketId == null) {
            // Tenant bağlamı yoksa tüm şirketlerin kullanıcıları döndürülmez (izolasyon).
            return Page.empty(pageable);
        }
        return kullaniciRepository.findBySirketId(sirketId, pageable).map(this::entityToDTO);
    }

    public KullaniciDTO getir(Long id) {
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        // Kullanıcı kendi kaydını her zaman okuyabilir (/ben). Şirket değiştirme sonrası
        // JWT'deki aktif şirket ile kayıtlı (home) şirket farklı olabildiği için burada
        // tenant kontrolü uygulanırsa kullanıcı kendi hesabına erişemez (self-lock).
        boolean kendisi = id.equals(tenantChecker.getCurrentKullaniciId());
        if (!kendisi) {
            tenantErisimiDogrula(k);
        }
        return entityToDTO(k);
    }

    /**
     * Kaydın, oturumdaki aktif şirket bağlamında erişilebilir olup olmadığını doğrular.
     * Aktif şirket kaydın home şirketi değilse, kullanıcının o şirkete gerçekten
     * atanmış olması (veya ADMIN olması) gerekir.
     */
    private void tenantErisimiDogrula(Kullanici k) {
        Long aktifSirketId = tenantChecker.getCurrentSirketId();
        if (aktifSirketId == null) {
            return;
        }
        if (aktifSirketId.equals(k.getSirketId())) {
            return;
        }
        if ("ADMIN".equals(k.getRole())) {
            return;
        }
        Set<Sirket> sirketler = k.getSirketler();
        boolean atanmis = sirketler != null && sirketler.stream()
                .anyMatch(s -> aktifSirketId.equals(s.getId()));
        if (!atanmis) {
            throw new ResourceNotFoundException("Kullanıcı bu sirkete ait degil");
        }
    }

    /**
     * <b>Platform geneli erişim</b> kontrolü — tüm şirketlerin verisini içeren
     * işlemler için (veritabanı yedekleme/geri yükleme, platform yapılandırması).
     *
     * <p>REDTEAM H-1: {@code /api/backups/**} yalnızca {@code hasRole('ADMIN')}
     * ile korunuyordu ve {@code BackupService} içinde {@code sirketId} hiç geçmiyordu.
     * Bu projede {@code Kullanici.role} <b>şirket başına</b> bir roldür ve platform
     * geneli superadmin kavramı YOKTUR. Testte kanıtlandı: Şirket B'nin ADMIN'i
     * Şirket 4'e ait TÜM veritabanı yedeklerini listeleyip indirebildi (yedek
     * tek DB dump'u olduğu için tüm şirketleri içerir) ve
     * {@code klasor=backups} ile presigned URL alabildi.
     *
     * <p><b>DÜZELTME:</b> Bu işlemler artık {@code app.platform.admin-users}
     * beyaz listesindeki kullanıcılara açıktır (fail-closed: liste boşsa kimse
     * erişemez). Beyaz liste <b>kullanıcı adına</b> göre tanımlanır ve ortam
     * değişkeni {@code APP_PLATFORM_ADMIN_USERS} ile verilir; varsayılan
     * {@code admin} (kurulumda oluşan ilk yönetici).
     *
     * @return {@code true} yalnızca gerçekten platform yöneticisiyse
     */
    public boolean platformYoneticisiMi() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return false;
        String username = auth.getName().trim();
        // ADMIN yetkisi tüm roller için geçerli olduğundan ikisini de kontrol et.
        if (!auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) return false;
        // Beyaz liste KULLANICI ADINA göredir; DB'deki rol tek başına yeterli
        // değildir (aksi halde her şirketin ADMIN'i platform erişimi kazanırdı).
        java.util.Set<String> platformAdminlar = platformAdminlar();
        if (!platformAdminlar.contains(username.toLowerCase(java.util.Locale.ROOT))) {
            if (log.isDebugEnabled()) {
                log.debug("Platform geneli işlem reddedildi (beyaz listede değil): {}", username);
            }
            return false;
        }
        // Beyaz listede olsa bile DB kaydı silinmişse erişim verilmez (fail-closed).
        return kullaniciRepository.findByUsername(username)
                .map(u -> "ADMIN".equals(u.getRole()) && Boolean.TRUE.equals(u.getActive()))
                .orElse(false);
    }

    /** Platform geneli işlem için yetki yoksa 403 fırlatır. */
    public void platformYoneticisiGerekir(String islem) {
        if (!platformYoneticisiMi()) {
            throw new com.raspel.erp.exception.BusinessException(
                    "Bu işlem yalnızca platform yöneticisi tarafından gerçekleştirilebilir: " + islem
                    + " ( Erişim verilen hesaplar: "
                    + (platformAdminlar().isEmpty() ? "<yapılandırılmamış>"
                       : String.join(", ", platformAdminlar())) + ")");
        }
    }

    public List<String> bildirimTercihleriGetir(Long kullaniciId) {
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", kullaniciId));
        return parseTercihler(k.getBildirimTercihleri());
    }

    public List<String> bildirimTercihleriGuncelle(Long kullaniciId, List<String> tercihler) {
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", kullaniciId));
        k.setBildirimTercihleri(serializeTercihler(tercihler));
        kullaniciRepository.save(k);
        return tercihler != null ? tercihler : List.of();
    }

    private List<String> parseTercihler(String ham) {
        if (ham == null || ham.isBlank()) return new java.util.ArrayList<>();
        return java.util.Arrays.stream(ham.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
    }

    private String serializeTercihler(List<String> tercihler) {
        if (tercihler == null || tercihler.isEmpty()) return null;
        return tercihler.stream().map(String::trim).filter(s -> !s.isBlank())
                .collect(Collectors.joining(","));
    }

    public KullaniciDTO olustur(KullaniciDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException("Kullanici sifresi zorunludur");
        }
        sifrePolitikasiKontrol(dto.getPassword());
        if (kullaniciRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new DuplicateResourceException("Bu kullanıcı adı zaten kullanılıyor: " + dto.getUsername());
        }
        Kullanici k = Kullanici.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .displayName(dto.getDisplayName())
                .avatarUrl(dto.getAvatarUrl())
                .companyName(dto.getCompanyName())
                .email(dto.getEmail())
                .sirketId(dto.getSirketId())
                .role(dto.getRole() != null ? dto.getRole() : "USER")
                .sahaKullanici(dto.getSahaKullanici() != null && dto.getSahaKullanici())
                .active(true)
                .build();
        setSirketler(k, dto.getSirketIds(), dto.getSirketId());
        return entityToDTO(kullaniciRepository.save(k));
    }

    public KullaniciDTO guncelle(Long id, KullaniciDTO dto) {
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        // getir() ile aynı esnek kontrol: aktif şirket home'dan farklı olsa da ADMIN/
        // atanmış kullanıcı düzenleyebilir (self-lock ve gereksiz 404 önlenir).
        tenantErisimiDogrula(k);
        // Kullanıcı adı yalnızca ADMIN tarafından ve benzersizlik kontrolüyle değiştirilebilir.
        if (dto.getUsername() != null && !dto.getUsername().isBlank()
                && !dto.getUsername().equals(k.getUsername())) {
            String yeniAd = dto.getUsername().trim();
            kullaniciRepository.findByUsername(yeniAd).ifPresent(mevcut -> {
                if (!mevcut.getId().equals(k.getId())) {
                    throw new DuplicateResourceException("Bu kullanıcı adı zaten kullanılıyor: " + yeniAd);
                }
            });
            k.setUsername(yeniAd);
        }
        if (dto.getDisplayName() != null) k.setDisplayName(dto.getDisplayName());
        if (dto.getAvatarUrl() != null) k.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getCompanyName() != null) k.setCompanyName(dto.getCompanyName());
        if (dto.getEmail() != null) k.setEmail(dto.getEmail());
        if (dto.getSirketId() != null) k.setSirketId(dto.getSirketId());
        if (dto.getSirketIds() != null) setSirketler(k, dto.getSirketIds(), dto.getSirketId());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            sifrePolitikasiKontrol(dto.getPassword());
            k.setPassword(passwordEncoder.encode(dto.getPassword()));
            // REDTEAM C9: Şifre değişiminde tokenVersion ARTIRILMIYORDU. Bu,
            // ele geçirilmiş bir hesabın parolası değiştirilse bile saldırganın
            // mevcut JWT'sinin geçerli kalmasına yol açıyordu (parola sıfırlama
            // olay müdahalesinin temel amacı iptaldir).
            tokenVersionArtir(k);
        }
        if (dto.getActive() != null) k.setActive(dto.getActive());
        if (dto.getRole() != null) k.setRole(dto.getRole());

        // REDTEAM C9: Kullanıcı bir şirketten çıkarıldığında (veya üye
        // şirket listesi değiştiğinde) eski JWT'leri geçerli kalıyordu; JWT'teki
        // sirketId claim'i DB'den teyit edilmediği için kullanıcı çıkarıldığı
        // şirketin verisine erişmeye devam ediyordu.
        if (dto.getSirketId() != null || dto.getSirketIds() != null) {
            tokenVersionArtir(k);
        }
        if (dto.getSahaKullanici() != null) k.setSahaKullanici(dto.getSahaKullanici());
        return entityToDTO(kullaniciRepository.save(k));
    }

    /**
     * Kullanıcının tüm mevcut oturumlarını geçersiz kılar (tokenVersion++).
     * {@code JwtAuthFilter} her istekte token sürümünü DB ile karşılaştırdığı için
     * artış, o kullanıcıya ait tüm JWT'leri geçersiz kılar.
     */
    private void tokenVersionArtir(Kullanici k) {
        k.setTokenVersion((k.getTokenVersion() != null ? k.getTokenVersion() : 0L) + 1);
        log.info("Kullanıcı oturumları geçersiz kılındı (tokenVersion++): id={}, username={}",
                k.getId(), k.getUsername());
    }

    /** Oturum açmış kullanıcının kendi profil güncellemesi: rol, aktiflik ve şifre değiştirilemez (şifre için ayrı endpoint). */
    public KullaniciDTO profilGuncelle(Long id, KullaniciDTO dto) {
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        if (dto.getDisplayName() != null) {
            if (dto.getDisplayName().isBlank()) {
                throw new com.raspel.erp.exception.BusinessException("Görünüm adı boş olamaz");
            }
            k.setDisplayName(dto.getDisplayName());
        }
        if (dto.getAvatarUrl() != null) k.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getCompanyName() != null) k.setCompanyName(dto.getCompanyName());
        return entityToDTO(kullaniciRepository.save(k));
    }

    public void sil(Long id) {
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        tenantErisimiDogrula(k);
        kullaniciRepository.delete(k);
    }

    public void sifreDegistir(Long id, SifreDegistirRequest req) {
        sifrePolitikasiKontrol(req.getYeniSifre());
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        if (!passwordEncoder.matches(req.getMevcutSifre(), k.getPassword())) {
            throw new BusinessException("Mevcut şifre hatalı");
        }
        k.setPassword(passwordEncoder.encode(req.getYeniSifre()));
        k.setTokenVersion((k.getTokenVersion() != null ? k.getTokenVersion() : 0L) + 1);
        kullaniciRepository.save(k);
    }

    public void sifreSifirla(com.raspel.erp.dto.sistem.SifreSifirlaRequest req) {
        sifrePolitikasiKontrol(req.getYeniSifre());
        Kullanici k = kullaniciRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı adı bulunamadı: " + req.getUsername()));
        tenantChecker.check(k.getSirketId(), "Kullanıcı");
        k.setPassword(passwordEncoder.encode(req.getYeniSifre()));
        k.setTokenVersion((k.getTokenVersion() != null ? k.getTokenVersion() : 0L) + 1);
        kullaniciRepository.save(k);
        log.info("Kullanıcı şifresi sıfırlandı: {}", req.getUsername());
    }

    /**
     * Kritik işlemler (örn. veritabanı geri yükleme) öncesi kimlik yeniden doğrulaması.
     */
    public void sifreDogrula(Long kullaniciId, String sifre) {
        if (kullaniciId == null) {
            throw new BusinessException("Kullanıcı kimliği doğrulanamadı");
        }
        if (sifre == null || sifre.isBlank()) {
            throw new BusinessException("Güvenlik doğrulaması başarısız: şifre hatalı");
        }
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", kullaniciId));
        if (!passwordEncoder.matches(sifre, k.getPassword())) {
            throw new BusinessException("Güvenlik doğrulaması başarısız: şifre hatalı");
        }
    }

    /**
     * Şifre sıfırlama talebi: kullanıcı adına kayıtlı e-posta adresine tek kullanımlık
     * bağlantı gönderir. Güvenlik gereği kullanıcı/e-posta varlığı sızdırılmaz:
     * kullanıcı yoksa veya e-posta tanımlı değilse sessizce döner.
     */
    public void sifreSifirlamaTalebi(String username) {
        if (username == null || username.isBlank()) return;
        Kullanici k = kullaniciRepository.findByUsername(username.trim()).orElse(null);
        if (k == null || k.getEmail() == null || k.getEmail().isBlank()) {
            log.info("Şifre sıfırlama talebi: kullanıcı/e-posta yok, sessizce dönüldü ({})", username);
            return;
        }

        String hamToken = java.util.UUID.randomUUID().toString().replace("-", "")
                + java.util.UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(hamToken);
        sifreSifirlaTokenRepository.kullaniciTokenlariniGecersizKil(k.getId());
        sifreSifirlaTokenRepository.save(com.raspel.erp.entity.sistem.SifreSifirlaToken.builder()
                .kullaniciId(k.getId())
                .tokenHash(tokenHash)
                .sonKullanma(java.time.LocalDateTime.now()
                        .plus(java.time.Duration.ofMillis(SIFRE_TOKEN_GECERLILIK_MS)))
                .kullanildi(false)
                .build());

        String taban = frontendUrl != null && !frontendUrl.isBlank() ? frontendUrl : "http://localhost:5173";
        String baglanti = taban.replaceAll("/+$", "") + "/sifre-sifirla?token=" + hamToken;
        String konu = "RasPel ERP - Şifre Sıfırlama";
        String html = "<p>Sayın " + (k.getDisplayName() != null ? k.getDisplayName() : k.getUsername()) + ",</p>"
                + "<p>Şifrenizi sıfırlamak için aşağıdaki bağlantıya tıklayın. Bağlantı 1 saat geçerlidir ve tek kullanımlıktır.</p>"
                + "<p><a href=\"" + baglanti + "\">Şifremi sıfırla</a></p>"
                + "<p>Bu talebi siz yapmadıysanız bu e-postayı dikkate almayın.</p>"
                + "<p>RasPel ERP</p>";
        boolean gonderildi = emailService.htmlGonder(k.getEmail(), konu, html);
        if (!gonderildi) {
            log.warn("Şifre sıfırlama e-postası gönderilemedi (SMTP yapılandırılmamış veya hata): {}", k.getUsername());
        }
    }

    /**
     * Şifre sıfırlama onayı: token doğrulanır, tek kullanımlık olduğu işaretlenir ve
     * yeni şifre kaydedilir. Tüm oturumlar geçersiz kılınır (tokenVersion artırılır).
     */
    public void sifreSifirlamaOnayla(String hamToken, String yeniSifre) {
        sifrePolitikasiKontrol(yeniSifre);
        if (hamToken == null || hamToken.isBlank()) {
            throw new BusinessException("Geçersiz sıfırlama bağlantısı");
        }
        var token = sifreSifirlaTokenRepository.findByTokenHash(hashToken(hamToken))
                .orElseThrow(() -> new BusinessException("Geçersiz veya kullanılmış sıfırlama bağlantısı"));
        if (Boolean.TRUE.equals(token.getKullanildi())
                || token.getSonKullanma() == null
                || token.getSonKullanma().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException("Sıfırlama bağlantısının süresi dolmuş veya kullanılmış");
        }
        // Faz 0.12: tek-kullanim garantisi atomik isaretleme ile saglanir. Ayni
        // token'la iki es zamanli istek gelirse yalnizca biri 1 satir gunceller.
        int isaretlendi = sifreSifirlaTokenRepository.tokenKullanildiIsaretle(token.getId());
        if (isaretlendi == 0) {
            throw new BusinessException("Sıfırlama bağlantısı zaten kullanılmış");
        }
        Long kullaniciId = token.getKullaniciId();
        token.setKullanildi(true);
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", kullaniciId));
        k.setPassword(passwordEncoder.encode(yeniSifre));
        k.setTokenVersion((k.getTokenVersion() != null ? k.getTokenVersion() : 0L) + 1);
        kullaniciRepository.save(k);
        log.info("Şifre sıfırlama tamamlandı: {}", k.getUsername());
    }

    private String hashToken(String hamToken) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(hamToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new BusinessException("Token hashlenemedi");
        }
    }

    public com.raspel.erp.dto.sistem.TwoFactorDTO setupTwoFactor(Long kullaniciId) {
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", kullaniciId));
        if (k.getTwoFactorEnabled() != null && k.getTwoFactorEnabled()) {
            throw new BusinessException("2FA zaten aktif. Önce devre dışı bırakın.");
        }
        String secret = TotpUtil.generateSecret();
        k.setTwoFactorSecret(secret);
        kullaniciRepository.save(k);
        String qrCodeUri = TotpUtil.otpauthUri("RasPelERP", k.getUsername(), secret);
        return com.raspel.erp.dto.sistem.TwoFactorDTO.builder()
                .enabled(false)
                .secret(secret)
                .qrCodeUri(qrCodeUri)
                .build();
    }

    public void enableTwoFactor(Long kullaniciId, String code) {
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", kullaniciId));
        if (k.getTwoFactorSecret() == null || k.getTwoFactorSecret().isBlank()) {
            throw new BusinessException("Önce 2FA kurulumu yapılmalıdır");
        }
        if (!TotpUtil.validate(k.getTwoFactorSecret(), code, System.currentTimeMillis())) {
            throw new BusinessException("Doğrulama kodu geçersiz veya süresi dolmuş");
        }
        k.setTwoFactorEnabled(true);
        kullaniciRepository.save(k);
        log.info("Kullanıcı için 2FA aktif edildi: {}", k.getUsername());
    }

    public void disableTwoFactor(Long kullaniciId, String code) {
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", kullaniciId));
        if (k.getTwoFactorEnabled() == null || !k.getTwoFactorEnabled()) {
            throw new BusinessException("2FA zaten kapalı");
        }
        if (!TotpUtil.validate(k.getTwoFactorSecret(), code, System.currentTimeMillis())) {
            throw new BusinessException("Doğrulama kodu geçersiz veya süresi dolmuş");
        }
        k.setTwoFactorEnabled(false);
        k.setTwoFactorSecret(null);
        kullaniciRepository.save(k);
        log.info("Kullanıcı için 2FA devre dışı bırakıldı: {}", k.getUsername());
    }

    /**
     * Kullanıcı varlığını sızdırmayan giriş doğrulaması.
     *
 * <p><b>Numaralandırma (enumeration) düzeltmesi:</b> Pasif bir kullanıcı giriş
 * denediğinde "Bu kullanıcı aktif değil" mesajı dönüyordu; olmayan kullanıcıda
 * ise "Kullanıcı adı veya şifre hatalı" dönüyordu. Farklı mesaj, saldırıya
 * geçerli kullanıcı adlarını listeleme imkânı veriyordu. Artık tüm başarısız
 * durumlar (kullanıcı yok / pasif / yanlış şifre) AYNI mesajı döner.
 *
 * <p><b>Zamanlama (timing) düzeltmesi:</b> Kullanıcı bulunamazsa BCrypt
 * karşılaştırması hiç çalışmıyordu; istek ~1 ms'de dönüyordu. Gerçek bir şifre
 * kontrolü BCrypt ile ~100 ms sürer. Fark, ölçüm yapan bir saldırganla
 * kullanıcı adlarını ayırt etmeye yeter. Bu yüzden kullanıcı bulunamasa bile
 * sabit bir BCrypt karşılaştırması (dummy) yapılır.
 */
/**
     * Tüm başarısız giriş durumları için tek mesaj. Farklı mesajlar kullanıcı
     * adlarının numaralandırılmasına (enumeration) yol açar.
     */
    private static final String GIRIS_HATASI = "Kullanıcı adı veya şifre hatalı";

    /**
     * Kullanıcı bulunamadığında gerçek bir BCrypt karşılaştırması kadar süre
     * harcanmasını sağlayan "kül" hash. Değeri kimseyle eşleşmez; amacı yalnızca
     * yanıt süresini eşitlemek (BCrypt kasıtlı olarak yavaştır).
     */
    private static final String ZAMANLAMA_KUL_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    /** Kullanıcı bulunamadığında yanıt süresini normal şifre kontrolüyle eşitler. */
    private void sifreDogrulaZamanlamaEsla(String sifre) {
        try {
            passwordEncoder.matches(sifre != null ? sifre : "", ZAMANLAMA_KUL_HASH);
        } catch (Exception e) {
            // Hash bozuksa sessizce geç; kimlik doğrulama zaten başarısız.
            log.debug("Zamanlama eşleme hash'i işlenemedi: {}", e.getMessage());
        }
    }

    public LoginResponse giris(LoginRequest req) {
    log.info("Giriş denemesi: {}", req.getUsername());
    java.util.Optional<Kullanici> bulunan = kullaniciRepository.findByUsername(req.getUsername());

    if (bulunan.isEmpty()) {
        // Zamanlama eşitlemesi: gerçek bir BCrypt doğrulaması kadar süre harcanır.
        sifreDogrulaZamanlamaEsla(req.getPassword());
        log.warn("BAŞARISIZ GİRİŞ - kullanıcı bulunamadı: {}", req.getUsername());
        girisSayaci("kullanici_yok");
        throw new BusinessException(GIRIS_HATASI);
    }

    Kullanici k = bulunan.get();

    if (!passwordEncoder.matches(req.getPassword(), k.getPassword())) {
        log.warn("BAŞARISIZ GİRİŞ - hatalı şifre: {}", req.getUsername());
        girisSayaci("hatali_sifre");
        auditLogService.log(k.getId(), k.getSirketId(), "LOGIN_FAILED", "Kullanici", k.getId(),
                "Başarısız giriş (hatalı şifre): " + k.getUsername(), null);
        throw new BusinessException(GIRIS_HATASI);
    }

    // Sıralama: önce şifre doğrulanır, sonra pasiflik kontrolü yapılır. Aksi halde
    // doğru şifreye sahip olmayan biri bile "pasif" bilgisini öğrenebilirdi.
    if (!k.getActive()) {
        log.warn("BAŞARISIZ GİRİŞ - pasif kullanıcı: {}", req.getUsername());
        girisSayaci("pasif");
        throw new BusinessException(GIRIS_HATASI);
    }

        girisSayaci("basarili");
        log.info("Başarılı giriş: {}", req.getUsername());
        // Kalıcı denetim izi: kimlik doğrulama olayları audit_log'a yazılır (forensics/uyum).
        auditLogService.log(k.getId(), k.getSirketId(), "LOGIN", "Kullanici", k.getId(),
                "Başarılı giriş: " + k.getUsername(), null);

        boolean twoFactorAktif = k.getTwoFactorEnabled() != null && k.getTwoFactorEnabled();

        String girisToken = UUID.randomUUID().toString();
        // 2FA aktifse token "henuz dogrulanmadi" olarak isaretlenir; giris-sirket bu token ile
        // JWT uretemez. JWT ancak giris-2fa adimiyla uretilen dogrulanmis token ile alinir.
        bekleyenKaydet(girisToken, k.getId(), System.currentTimeMillis(), !twoFactorAktif);
        List<com.raspel.erp.dto.sistem.SirketDTO> sirketler = getSirketlerForKullanici(k);

        return LoginResponse.builder()
                .id(k.getId())
                .username(k.getUsername())
                .displayName(k.getDisplayName())
                .avatarUrl(k.getAvatarUrl())
                .role(k.getRole()).sahaKullanici(k.getSahaKullanici())
                .personelId(personelIdBul(k))
                .twoFactorGerekli(twoFactorAktif)
                .girisToken(girisToken)
                .sirketler(twoFactorAktif ? null : sirketler)
                .build();
    }

    /**
 * TOTP replay kontrolü.
 *
     * <p>Doğrulama penceresi ±1 adım (±30 sn) olduğu için geçerli bir kod üç kez
     * kabul edilebiliyordu. Ekrandan görülen bir kod (omuz sörfü, ekran görüntüsü)
     * tekrar kullanılabiliyordu. Artık kullanılan zaman adımı saklanıyor ve aynı
     * adımla gelen ikinci istek reddediliyor.
     *
     * @return true ise bu kod daha önce kullanılmıştır (replay)
     */
    private boolean replayKontrolu(Kullanici k, long counter) {
        // Faz 0.11: atomik kosullu guncelleme. Iki es zamanli istekten yalnizca
        // biri satiri gunceller; digeri 0 satir alir -> replay.
        int guncellenen = kullaniciRepository.totpCounterGuncelle(k.getId(), counter);
        if (guncellenen == 0) {
            log.warn("TOTP replay denemesi - kullanıcı: {}, counter: {}",
                    k.getUsername(), counter);
            return true;
        }
        k.setTwoFactorLastCounter(counter);
        return false;
    }

    public LoginResponse giris2faTamamla(TwoFactorGirisRequest req) {
        if (req == null || req.getGirisToken() == null || req.getGirisToken().isBlank()) {
            throw new BusinessException("Giriş oturumu bulunamadı, tekrar giriş yapınız");
        }
        long[] kayit = bekleyenGetir(req.getGirisToken());
        if (kayit == null) {
            throw new BusinessException("Giriş oturumu bulunamadı, tekrar giriş yapınız");
        }
        long kullaniciId = kayit[0];
        long olusturmaZamani = kayit[1];
        if (System.currentTimeMillis() - olusturmaZamani > GIRIS_TOKEN_GECERLILIK_MS) {
            bekleyenSil(req.getGirisToken());
            throw new BusinessException("Giriş kodunun süresi doldu, tekrar giriş yapınız");
        }

        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));
        if (!k.getActive()) throw new BusinessException("Bu kullanıcı aktif değil");
        if (k.getTwoFactorEnabled() == null || !k.getTwoFactorEnabled()) {
            throw new BusinessException("2FA aktif değil");
        }
        TotpUtil.DogrulamaSonucu dogrulama = TotpUtil.dogrula(k.getTwoFactorSecret(), req.getCode(), System.currentTimeMillis());
        // Replay koruması: aynı zaman adımına ait kod ikinci kez kabul edilmez.
        // Pencere ±30 sn olduğu için aynı kod üç kere kullanılabiliyordu.
        if (dogrulama.gecerli() && replayKontrolu(k, dogrulama.counter())) {
            throw new BusinessException("Bu doğrulama kodu zaten kullanıldı. Lütfen uygulamadaki yeni kodu girin.");
        }
        if (!dogrulama.gecerli()) {
            girisSayaci("hatali_2fa_kodu");
            throw new BusinessException("Doğrulama kodu geçersiz veya süresi dolmuş");
        }

        bekleyenSil(req.getGirisToken());
        String yeniToken = UUID.randomUUID().toString();
        bekleyenKaydet(yeniToken, k.getId(), System.currentTimeMillis());
        girisSayaci("basarili_2fa");

        List<com.raspel.erp.dto.sistem.SirketDTO> sirketler = getSirketlerForKullanici(k);
        return LoginResponse.builder()
                .id(k.getId())
                .username(k.getUsername())
                .displayName(k.getDisplayName())
                .avatarUrl(k.getAvatarUrl())
                .role(k.getRole()).sahaKullanici(k.getSahaKullanici())
                .personelId(personelIdBul(k))
                .twoFactorGerekli(false)
                .girisToken(yeniToken)
                .sirketler(sirketler)
                .build();
    }

    public LoginResponse girisSirket(String girisToken, Long sirketId) {
        if (girisToken == null || girisToken.isBlank()) {
            throw new BusinessException("Giriş oturumu bulunamadı, tekrar giriş yapınız");
        }
        long[] kayit = bekleyenGetir(girisToken);
        if (kayit == null) {
            throw new BusinessException("Giriş oturumu bulunamadı, tekrar giriş yapınız");
        }
        long kullaniciId = kayit[0];
        long olusturmaZamani = kayit[1];
        if (System.currentTimeMillis() - olusturmaZamani > GIRIS_TOKEN_GECERLILIK_MS) {
            bekleyenSil(girisToken);
            throw new BusinessException("Şirket seçim süresi doldu, tekrar giriş yapınız");
        }
        bekleyenSil(girisToken);

        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));
        if (!k.getActive()) throw new BusinessException("Bu kullanıcı aktif değil");

        // 2FA aktifse, sifre adimindan gelen (dogrulanmamis) token ile JWT uretilemez.
        boolean twoFactorAktif = Boolean.TRUE.equals(k.getTwoFactorEnabled());
        boolean ikiFaktorDogrulandi = kayit.length > 2 && kayit[2] == 1L;
        if (twoFactorAktif && !ikiFaktorDogrulandi) {
            throw new BusinessException("İki adımlı doğrulama gerekli. Lütfen doğrulama kodunu giriniz.");
        }

        if (sirketId == null) {
            sirketId = k.getSirketId();
        }
        final Long secilenSirketId = sirketId;
        if (secilenSirketId == null) {
            throw new BusinessException("Şirket seçimi zorunludur, lütfen tekrar giriş yapınız");
        }

        // Admin herhangi bir aktif firmayı seçebilir; USER yalnızca atandığı firmada oturum açabilir
        if (!"ADMIN".equals(k.getRole())) {
            boolean uye = k.getSirketler() != null && k.getSirketler().stream()
                    .anyMatch(s -> s.getId().equals(secilenSirketId));
            if (!uye && (k.getSirketId() == null || !k.getSirketId().equals(secilenSirketId))) {
                throw new BusinessException("Bu şirkette çalışma yetkiniz yok");
            }
        }

        auditLogService.log(k.getId(), secilenSirketId, "LOGIN", "Kullanici", k.getId(),
                "Oturum açıldı: " + k.getUsername(), null);
        return tokenOlusturVeDon(k, null, sirketId);
    }

    private List<com.raspel.erp.dto.sistem.SirketDTO> getSirketlerForKullanici(Kullanici k) {
        if ("ADMIN".equals(k.getRole())) {
            return sirketRepository.findByAktifTrue().stream()
                    .map(this::sirketToDTO)
                    .collect(Collectors.toList());
        }
        Set<Sirket> sirketler = k.getSirketler();
        if (sirketler != null && !sirketler.isEmpty()) {
            return sirketler.stream()
                    .map(this::sirketToDTO)
                    .collect(Collectors.toList());
        }
        if (k.getSirketId() != null) {
            return sirketRepository.findById(k.getSirketId())
                    .map(s -> List.of(sirketToDTO(s)))
                    .orElse(List.of());
        }
        return List.of();
    }

    private com.raspel.erp.dto.sistem.SirketDTO sirketToDTO(Sirket s) {
        return com.raspel.erp.dto.sistem.SirketDTO.builder()
                .id(s.getId()).ad(s.getAd()).vergiNo(s.getVergiNo())
                .tur(s.getTur()).yil(s.getYil()).logoUrl(s.getLogoUrl())
                .build();
    }

    /** Oturum açmış kullanıcının erişebileceği şirketleri döndürür. */
    @Transactional(readOnly = true)
    public List<com.raspel.erp.dto.sistem.SirketDTO> sirketlerim(Long kullaniciId) {
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));
        return getSirketlerForKullanici(k);
    }

    /** Oturum açıkken şirket değiştirir; yeni JWT üretir. */
    public LoginResponse sirketDegistir(Long kullaniciId, Long sirketId) {
        if (sirketId == null) {
            throw new BusinessException("Şirket seçimi zorunludur");
        }
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));
        if (!k.getActive()) throw new BusinessException("Bu kullanıcı aktif değil");

        // Admin herhangi bir aktif firmaya geçebilir; USER yalnızca atandığı firmalara
        if (!"ADMIN".equals(k.getRole())) {
            boolean uye = k.getSirketler() != null && k.getSirketler().stream()
                    .anyMatch(s -> s.getId().equals(sirketId));
            if (!uye && (k.getSirketId() == null || !k.getSirketId().equals(sirketId))) {
                throw new BusinessException("Bu şirkette çalışma yetkiniz yok");
            }
        }
        return tokenOlusturVeDon(k, null, sirketId);
    }

    private Long personelIdBul(Kullanici k) {
        if (k == null) return null;
        try {
            return personelRepository.findByKullaniciId(k.getId()).map(p -> p.getId()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static final long OTURUM_UZATMA_MS = 30 * 60 * 1000L; // "Uzat" basina +30 dk
    private static final long OTURUM_MAX_MS = 2 * 60 * 60 * 1000L;  // mutlak ust sinir (suiistimali onler)

    /** Oturum acikken suresi bitmek uzereyse 30 dk daha verir; ust sinir 2 saattir. */
    public LoginResponse oturumUzat(Long kullaniciId, String mevcutToken) {
        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));
        if (!k.getActive()) throw new BusinessException("Bu kullanıcı aktif değil");
        long simdi = System.currentTimeMillis();
        Long mevcutBitis = mevcutToken != null ? jwtUtil.getExpirationFromToken(mevcutToken) : null;
        long temel = (mevcutBitis != null && mevcutBitis > simdi) ? mevcutBitis : simdi;
        long hedef = Math.min(temel + OTURUM_UZATMA_MS, simdi + OTURUM_MAX_MS);
        // Aktif şirket, mevcut token'dan korunur; home şirketine düşürülmez
        // (aksi halde şirket değiştirdikten sonra liste/tenant bağlamı bozulur).
        Long aktifSirketId = mevcutToken != null ? jwtUtil.getSirketIdFromToken(mevcutToken) : null;
        LoginResponse yanit = tokenOlusturVeDon(k, null, aktifSirketId, hedef);
        // Eski token'i iptal et (yeni token ile oturum devam eder).
        try {
            if (mevcutToken != null) {
                String eskiJti = jwtUtil.getJtiFromToken(mevcutToken);
                if (eskiJti != null) aktifOturumService.oturumIptal(eskiJti);
            }
        } catch (Exception ignored) {
            // Eski token iptal edilemezse yeni token yine de gecerlidir.
        }
        return yanit;
    }

    private LoginResponse tokenOlusturVeDon(Kullanici k, String istekFirma, Long istekSirketId) {
        return tokenOlusturVeDon(k, istekFirma, istekSirketId, null);
    }

    private LoginResponse tokenOlusturVeDon(Kullanici k, String istekFirma, Long istekSirketId, Long ozelBitisMs) {
        String company = istekFirma != null && !istekFirma.isBlank() ? istekFirma : k.getCompanyName();
        Long sirketId = istekSirketId != null ? istekSirketId : k.getSirketId();
        String sirketAdi = null;
        if (sirketId != null) {
            sirketAdi = sirketRepository.findById(sirketId).map(Sirket::getAd).orElse(null);
        }
        long bitisMs = ozelBitisMs != null ? ozelBitisMs : System.currentTimeMillis() + jwtExpirationMs;
        long gecerlilikMs = Math.max(bitisMs - System.currentTimeMillis(), 1000L);
        // Ozel bitis verilmediyse varsayilan sureli token uret (mevcut davranis korunur).
        String token = ozelBitisMs != null
                ? jwtUtil.generateToken(k, sirketId, sirketAdi, gecerlilikMs)
                : jwtUtil.generateToken(k, sirketId, sirketAdi);
        aktifOturumKaydet(token, k, sirketId, ozelBitisMs != null ? gecerlilikMs : jwtExpirationMs);
        return LoginResponse.builder()
                .id(k.getId())
                .username(k.getUsername())
                .displayName(k.getDisplayName())
                .avatarUrl(k.getAvatarUrl())
                .sirketId(sirketId)
                .sirketAdi(sirketAdi)
                .companyName(company)
                .role(k.getRole()).sahaKullanici(k.getSahaKullanici())
                .personelId(personelIdBul(k))
                .token(token)
                .tokenExpiresAt(bitisMs)
                .twoFactorGerekli(false)
                .build();
    }

    private void aktifOturumKaydet(String token, Kullanici k, Long sirketId) {
        aktifOturumKaydet(token, k, sirketId, jwtExpirationMs);
    }

    private void aktifOturumKaydet(String token, Kullanici k, Long sirketId, long ttlMs) {
        try {
            String jti = jwtUtil.getJtiFromToken(token);
            String ip = null;
            try {
                HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
                ip = req.getRemoteAddr();
            } catch (Exception ignored) {
                // Request context yoksa (test/dahili çağrı) IP kaydedilmez
            }
            aktifOturumService.oturumKaydet(jti, k.getId(), k.getUsername(), sirketId, ip, Duration.ofMillis(ttlMs));
            // "En son giris kazanir": onceki oturum jti'si iptal edilir.
            aktifOturumService.aktifOturumAyarla(k.getId(), jti, Duration.ofMillis(ttlMs));
        } catch (Exception e) {
            log.warn("Aktif oturum kaydedilemedi: {}", e.getMessage());
        }
    }

    private KullaniciDTO entityToDTO(Kullanici k) {
        Set<Sirket> sirketler = k.getSirketler();
        return KullaniciDTO.builder()
                .id(k.getId()).username(k.getUsername())
                .displayName(k.getDisplayName()).avatarUrl(k.getAvatarUrl())
                .companyName(k.getCompanyName()).email(k.getEmail()).sirketId(k.getSirketId())
                .sirketIds(sirketler != null ? sirketler.stream().map(Sirket::getId).collect(Collectors.toList()) : List.of())
                .role(k.getRole()).sahaKullanici(k.getSahaKullanici()).active(k.getActive())
                .twoFactorEnabled(k.getTwoFactorEnabled() != null && k.getTwoFactorEnabled())
                .olusturmaTarihi(k.getOlusturmaTarihi())
                .build();
    }

    private void setSirketler(Kullanici k, List<Long> sirketIds, Long fallbackSirketId) {
        if (sirketIds != null && !sirketIds.isEmpty()) {
            Set<Sirket> sirketler = sirketIds.stream()
                    .map(id -> sirketRepository.findById(id).orElse(null))
                    .filter(s -> s != null)
                    .collect(Collectors.toSet());
            k.setSirketler(sirketler);
            k.setSirketId(sirketIds.get(0));
        } else if (fallbackSirketId != null) {
            k.setSirketId(fallbackSirketId);
        }
    }

    private void sifrePolitikasiKontrol(String sifre) {
        if (sifre.length() < 8) {
            throw new BusinessException("Sifre en az 8 karakter olmalidir");
        }
        if (sifre.length() > 72) {
            throw new BusinessException("Sifre en fazla 72 karakter olmalidir");
        }
        if (!sifre.matches(".*[A-Z].*")) {
            throw new BusinessException("Sifre en az bir buyuk harf icermelidir");
        }
        if (!sifre.matches(".*[a-z].*")) {
            throw new BusinessException("Sifre en az bir kucuk harf icermelidir");
        }
        if (!sifre.matches(".*[0-9].*")) {
            throw new BusinessException("Sifre en az bir rakam icermelidir");
        }
        if (!sifre.matches(".*[^a-zA-Z0-9].*")) {
            throw new BusinessException("Sifre en az bir ozel karakter icermelidir");
        }
    }
}