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

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private static final long SIFRE_TOKEN_GECERLILIK_MS = 60 * 60 * 1000; // 1 saat

    /** Bekleyen girişler: girisToken -> (kullaniciId, oluşturmaZamani). 5 dakika geçerli.
     *  Redis'te saklanır (sunucu restart'ında kaybolmaz); Redis'e erişilemezse bellek fallback'i kullanılır. */
    private final ConcurrentMap<String, long[]> bekleyenGirislerBellek = new ConcurrentHashMap<>();
    private static final long GIRIS_TOKEN_GECERLILIK_MS = 5 * 60 * 1000;
    private static final String GIRIS_REDIS_PREFIX = "giris:bekleyen:";

    private void bekleyenKaydet(String token, long kullaniciId, long zaman) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(GIRIS_REDIS_PREFIX + token,
                        kullaniciId + ":" + zaman, Duration.ofMillis(GIRIS_TOKEN_GECERLILIK_MS));
                return;
            }
        } catch (Exception e) {
            log.warn("Redis erişilemedi, giriş oturumu bellekte tutulacak: {}", e.getMessage());
        }
        bekleyenGirislerBellek.put(token, new long[]{kullaniciId, zaman});
    }

    private long[] bekleyenGetir(String token) {
        try {
            if (redisTemplate != null) {
                String val = redisTemplate.opsForValue().get(GIRIS_REDIS_PREFIX + token);
                if (val != null) {
                    String[] parcalar = val.split(":");
                    return new long[]{Long.parseLong(parcalar[0]), Long.parseLong(parcalar[1])};
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
        tenantChecker.check(k.getSirketId(), "Kullanıcı");
        return entityToDTO(k);
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
        tenantChecker.check(k.getSirketId(), "Kullanıcı");
        if (dto.getDisplayName() != null) k.setDisplayName(dto.getDisplayName());
        if (dto.getAvatarUrl() != null) k.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getCompanyName() != null) k.setCompanyName(dto.getCompanyName());
        if (dto.getEmail() != null) k.setEmail(dto.getEmail());
        if (dto.getSirketId() != null) k.setSirketId(dto.getSirketId());
        if (dto.getSirketIds() != null) setSirketler(k, dto.getSirketIds(), dto.getSirketId());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            sifrePolitikasiKontrol(dto.getPassword());
            k.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getActive() != null) k.setActive(dto.getActive());
        if (dto.getRole() != null) k.setRole(dto.getRole());
        if (dto.getSahaKullanici() != null) k.setSahaKullanici(dto.getSahaKullanici());
        return entityToDTO(kullaniciRepository.save(k));
    }

    /** Oturum açmış kullanıcının kendi profil güncellemesi: rol, aktiflik ve şifre değiştirilemez (şifre için ayrı endpoint). */
    public KullaniciDTO profilGuncelle(Long id, KullaniciDTO dto) {
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        if (dto.getDisplayName() != null) k.setDisplayName(dto.getDisplayName());
        if (dto.getAvatarUrl() != null) k.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getCompanyName() != null) k.setCompanyName(dto.getCompanyName());
        return entityToDTO(kullaniciRepository.save(k));
    }

    public void sil(Long id) {
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        tenantChecker.check(k.getSirketId(), "Kullanıcı");
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
        Kullanici k = kullaniciRepository.findById(token.getKullaniciId())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", token.getKullaniciId()));
        k.setPassword(passwordEncoder.encode(yeniSifre));
        k.setTokenVersion((k.getTokenVersion() != null ? k.getTokenVersion() : 0L) + 1);
        kullaniciRepository.save(k);
        token.setKullanildi(true);
        sifreSifirlaTokenRepository.save(token);
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

    public LoginResponse giris(LoginRequest req) {
        log.info("Giriş denemesi: {}", req.getUsername());
        Kullanici k = kullaniciRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> {
                    log.warn("BAŞARISIZ GİRİŞ - kullanıcı bulunamadı: {}", req.getUsername());
                    return new BusinessException("Kullanıcı adı veya şifre hatalı");
                });

        if (!k.getActive()) {
            log.warn("BAŞARISIZ GİRİŞ - pasif kullanıcı: {}", req.getUsername());
            throw new BusinessException("Bu kullanıcı aktif değil");
        }

        if (!passwordEncoder.matches(req.getPassword(), k.getPassword())) {
            log.warn("BAŞARISIZ GİRİŞ - hatalı şifre: {}", req.getUsername());
            throw new BusinessException("Kullanıcı adı veya şifre hatalı");
        }

        log.info("Başarılı giriş: {}", req.getUsername());

        String girisToken = UUID.randomUUID().toString();
        bekleyenKaydet(girisToken, k.getId(), System.currentTimeMillis());

        boolean twoFactorAktif = k.getTwoFactorEnabled() != null && k.getTwoFactorEnabled();
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
        if (!TotpUtil.validate(k.getTwoFactorSecret(), req.getCode(), System.currentTimeMillis())) {
            throw new BusinessException("Doğrulama kodu geçersiz veya süresi dolmuş");
        }

        bekleyenSil(req.getGirisToken());
        String yeniToken = UUID.randomUUID().toString();
        bekleyenKaydet(yeniToken, k.getId(), System.currentTimeMillis());

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

    private LoginResponse tokenOlusturVeDon(Kullanici k, String istekFirma, Long istekSirketId) {
        String company = istekFirma != null && !istekFirma.isBlank() ? istekFirma : k.getCompanyName();
        Long sirketId = istekSirketId != null ? istekSirketId : k.getSirketId();
        String sirketAdi = null;
        if (sirketId != null) {
            sirketAdi = sirketRepository.findById(sirketId).map(Sirket::getAd).orElse(null);
        }
        String token = jwtUtil.generateToken(k, sirketId, sirketAdi);
        aktifOturumKaydet(token, k, sirketId);
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
                .tokenExpiresAt(System.currentTimeMillis() + jwtExpirationMs)
                .twoFactorGerekli(false)
                .build();
    }

    private void aktifOturumKaydet(String token, Kullanici k, Long sirketId) {
        try {
            String jti = jwtUtil.getJtiFromToken(token);
            String ip = null;
            try {
                HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
                ip = req.getRemoteAddr();
            } catch (Exception ignored) {
                // Request context yoksa (test/dahili çağrı) IP kaydedilmez
            }
            aktifOturumService.oturumKaydet(jti, k.getId(), k.getUsername(), sirketId, ip, Duration.ofMillis(jwtExpirationMs));
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