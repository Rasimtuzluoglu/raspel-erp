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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

    /** Bekleyen 2FA girişleri: girisToken -> (kullaniciId, oluşturmaZamani). 5 dakika geçerli. */
    private final ConcurrentMap<String, long[]> bekleyenGirisler = new ConcurrentHashMap<>();
    private static final long GIRIS_TOKEN_GECERLILIK_MS = 5 * 60 * 1000;

    public Page<KullaniciDTO> tumunuGetir(Pageable pageable) {
        return kullaniciRepository.findAll(pageable).map(this::entityToDTO);
    }

    public KullaniciDTO getir(Long id) {
        return entityToDTO(kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id)));
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
                .sirketId(dto.getSirketId())
                .role(dto.getRole() != null ? dto.getRole() : "USER")
                .active(true)
                .build();
        setSirketler(k, dto.getSirketIds(), dto.getSirketId());
        return entityToDTO(kullaniciRepository.save(k));
    }

    public KullaniciDTO guncelle(Long id, KullaniciDTO dto) {
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        if (dto.getDisplayName() != null) k.setDisplayName(dto.getDisplayName());
        if (dto.getAvatarUrl() != null) k.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getCompanyName() != null) k.setCompanyName(dto.getCompanyName());
        if (dto.getSirketId() != null) k.setSirketId(dto.getSirketId());
        if (dto.getSirketIds() != null) setSirketler(k, dto.getSirketIds(), dto.getSirketId());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            sifrePolitikasiKontrol(dto.getPassword());
            k.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getActive() != null) k.setActive(dto.getActive());
        if (dto.getRole() != null) k.setRole(dto.getRole());
        return entityToDTO(kullaniciRepository.save(k));
    }

    /** Oturum açmış kullanıcının kendi profil güncellemesi: rol ve aktiflik değiştirilemez. */
    public KullaniciDTO profilGuncelle(Long id, KullaniciDTO dto) {
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        if (dto.getDisplayName() != null) k.setDisplayName(dto.getDisplayName());
        if (dto.getAvatarUrl() != null) k.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getCompanyName() != null) k.setCompanyName(dto.getCompanyName());
        if (dto.getSirketId() != null) k.setSirketId(dto.getSirketId());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) k.setPassword(passwordEncoder.encode(dto.getPassword()));
        return entityToDTO(kullaniciRepository.save(k));
    }

    public void sil(Long id) {
        if (!kullaniciRepository.existsById(id)) throw new ResourceNotFoundException("Kullanıcı", id);
        kullaniciRepository.deleteById(id);
    }

    public void sifreDegistir(Long id, SifreDegistirRequest req) {
        sifrePolitikasiKontrol(req.getYeniSifre());
        Kullanici k = kullaniciRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı", id));
        if (!passwordEncoder.matches(req.getMevcutSifre(), k.getPassword())) {
            throw new BusinessException("Mevcut şifre hatalı");
        }
        k.setPassword(passwordEncoder.encode(req.getYeniSifre()));
        kullaniciRepository.save(k);
    }

    public void sifreSifirla(com.raspel.erp.dto.sistem.SifreSifirlaRequest req) {
        Kullanici k = kullaniciRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı adı bulunamadı: " + req.getUsername()));
        k.setPassword(passwordEncoder.encode(req.getYeniSifre()));
        kullaniciRepository.save(k);
        log.info("Kullanıcı şifresi sıfırlandı: {}", req.getUsername());
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
                .orElseThrow(() -> new BusinessException("Kullanıcı adı veya şifre hatalı"));

        if (!k.getActive()) throw new BusinessException("Bu kullanıcı aktif değil");

        if (!passwordEncoder.matches(req.getPassword(), k.getPassword())) {
            throw new BusinessException("Kullanıcı adı veya şifre hatalı");
        }

        String girisToken = UUID.randomUUID().toString();
        bekleyenGirisler.put(girisToken, new long[]{k.getId(), System.currentTimeMillis()});

        boolean twoFactorAktif = k.getTwoFactorEnabled() != null && k.getTwoFactorEnabled();
        List<com.raspel.erp.dto.sistem.SirketDTO> sirketler = getSirketlerForKullanici(k);

        return LoginResponse.builder()
                .id(k.getId())
                .username(k.getUsername())
                .displayName(k.getDisplayName())
                .avatarUrl(k.getAvatarUrl())
                .role(k.getRole())
                .twoFactorGerekli(twoFactorAktif)
                .girisToken(girisToken)
                .sirketler(twoFactorAktif ? null : sirketler)
                .build();
    }

    public LoginResponse giris2faTamamla(TwoFactorGirisRequest req) {
        long[] kayit = bekleyenGirisler.get(req.getGirisToken());
        if (kayit == null) {
            throw new BusinessException("Giriş oturumu bulunamadı, tekrar giriş yapınız");
        }
        long kullaniciId = kayit[0];
        long olusturmaZamani = kayit[1];
        if (System.currentTimeMillis() - olusturmaZamani > GIRIS_TOKEN_GECERLILIK_MS) {
            bekleyenGirisler.remove(req.getGirisToken());
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

        bekleyenGirisler.remove(req.getGirisToken());
        String yeniToken = UUID.randomUUID().toString();
        bekleyenGirisler.put(yeniToken, new long[]{k.getId(), System.currentTimeMillis()});

        List<com.raspel.erp.dto.sistem.SirketDTO> sirketler = getSirketlerForKullanici(k);
        return LoginResponse.builder()
                .id(k.getId())
                .username(k.getUsername())
                .displayName(k.getDisplayName())
                .avatarUrl(k.getAvatarUrl())
                .role(k.getRole())
                .twoFactorGerekli(false)
                .girisToken(yeniToken)
                .sirketler(sirketler)
                .build();
    }

    public LoginResponse girisSirket(String girisToken, Long sirketId) {
        long[] kayit = bekleyenGirisler.get(girisToken);
        if (kayit == null) {
            throw new BusinessException("Giriş oturumu bulunamadı, tekrar giriş yapınız");
        }
        long kullaniciId = kayit[0];
        long olusturmaZamani = kayit[1];
        if (System.currentTimeMillis() - olusturmaZamani > GIRIS_TOKEN_GECERLILIK_MS) {
            bekleyenGirisler.remove(girisToken);
            throw new BusinessException("Şirket seçim süresi doldu, tekrar giriş yapınız");
        }
        bekleyenGirisler.remove(girisToken);

        Kullanici k = kullaniciRepository.findById(kullaniciId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı"));
        if (!k.getActive()) throw new BusinessException("Bu kullanıcı aktif değil");

        return tokenOlusturVeDon(k, null, sirketId);
    }

    private List<com.raspel.erp.dto.sistem.SirketDTO> getSirketlerForKullanici(Kullanici k) {
        if ("ADMIN".equals(k.getRole())) {
            return sirketRepository.findByAktifTrue().stream()
                    .map(s -> com.raspel.erp.dto.sistem.SirketDTO.builder()
                            .id(s.getId()).ad(s.getAd()).logoUrl(s.getLogoUrl()).build())
                    .collect(Collectors.toList());
        }
        Set<Sirket> sirketler = k.getSirketler();
        if (sirketler != null && !sirketler.isEmpty()) {
            return sirketler.stream()
                    .map(s -> com.raspel.erp.dto.sistem.SirketDTO.builder()
                            .id(s.getId()).ad(s.getAd()).logoUrl(s.getLogoUrl()).build())
                    .collect(Collectors.toList());
        }
        if (k.getSirketId() != null) {
            return sirketRepository.findById(k.getSirketId())
                    .map(s -> List.of(com.raspel.erp.dto.sistem.SirketDTO.builder()
                            .id(s.getId()).ad(s.getAd()).logoUrl(s.getLogoUrl()).build()))
                    .orElse(List.of());
        }
        return List.of();
    }

    private LoginResponse tokenOlusturVeDon(Kullanici k, String istekFirma, Long istekSirketId) {
        String company = istekFirma != null && !istekFirma.isBlank() ? istekFirma : k.getCompanyName();
        Long sirketId = istekSirketId != null ? istekSirketId : k.getSirketId();
        String sirketAdi = null;
        if (sirketId != null) {
            sirketAdi = sirketRepository.findById(sirketId).map(Sirket::getAd).orElse(null);
        }
        String token = jwtUtil.generateToken(k, sirketId, sirketAdi);
        return LoginResponse.builder()
                .id(k.getId())
                .username(k.getUsername())
                .displayName(k.getDisplayName())
                .avatarUrl(k.getAvatarUrl())
                .sirketId(sirketId)
                .sirketAdi(sirketAdi)
                .companyName(company)
                .role(k.getRole())
                .token(token)
                .twoFactorGerekli(false)
                .build();
    }

    private KullaniciDTO entityToDTO(Kullanici k) {
        Set<Sirket> sirketler = k.getSirketler();
        return KullaniciDTO.builder()
                .id(k.getId()).username(k.getUsername())
                .displayName(k.getDisplayName()).avatarUrl(k.getAvatarUrl())
                .companyName(k.getCompanyName()).sirketId(k.getSirketId())
                .sirketIds(sirketler != null ? sirketler.stream().map(Sirket::getId).collect(Collectors.toList()) : List.of())
                .role(k.getRole()).active(k.getActive())
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
        if (!sifre.matches(".*[A-Z].*")) {
            throw new BusinessException("Sifre en az bir buyuk harf icermelidir");
        }
        if (!sifre.matches(".*[a-z].*")) {
            throw new BusinessException("Sifre en az bir kucuk harf icermelidir");
        }
        if (!sifre.matches(".*[0-9].*")) {
            throw new BusinessException("Sifre en az bir rakam icermelidir");
        }
    }
}