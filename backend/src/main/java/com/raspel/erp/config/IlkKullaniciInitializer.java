package com.raspel.erp.config;

import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.entity.sistem.Sirket;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import com.raspel.erp.repository.sistem.SirketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * İlk kurulum:
 * - Veritabanında hiç aktif firma yoksa varsayılan bir firma oluşturur.
 * - Hiç kullanıcı yoksa varsayılan ADMIN kullanıcısını oluşturur ve firmaya atar.
 * - Admin kullanıcısı var ama firmaya atanmamışsa (örn. eski kurulum) varsayılan firmaya atar.
 * Kullanıcı adı/şifre ortam değişkenleriyle özelleştirilebilir (APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IlkKullaniciInitializer implements CommandLineRunner {

    private final KullaniciRepository kullaniciRepository;
    private final SirketRepository sirketRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.display-name:Yönetici}")
    private String adminDisplayName;

    @Value("${app.admin.company-name:RasPel Şirketi}")
    private String adminCompanyName;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Aktif firma yoksa varsayılan firma oluştur
        Sirket sirket = sirketRepository.findByAktifTrue().stream().findFirst().orElse(null);
        if (sirket == null) {
            sirket = Sirket.builder()
                    .ad(adminCompanyName)
                    .vergiNo("0000000000")
                    .aktif(true)
                    .build();
            sirket = sirketRepository.save(sirket);
            log.info("Varsayilan sirket olusturuldu: {} (ID: {})", sirket.getAd(), sirket.getId());
        }

        // 2. Admin kullanıcısı: yoksa oluştur, varsa ama firmaya atanmamışsa ata
        Optional<Kullanici> adminOpt = kullaniciRepository.findByUsername(adminUsername);
        if (adminOpt.isPresent()) {
            Kullanici admin = adminOpt.get();
            boolean firmaAtanmamis = admin.getSirketId() == null
                    && (admin.getSirketler() == null || admin.getSirketler().isEmpty());
            if (firmaAtanmamis) {
                admin.setSirketId(sirket.getId());
                admin.setCompanyName(sirket.getAd());
                admin.setSirketler(new HashSet<>(Set.of(sirket)));
                kullaniciRepository.save(admin);
                log.info("Mevcut admin kullanicisi varsayilan firmaya atandi: {}", sirket.getAd());
            }
        } else {
            Kullanici admin = Kullanici.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .displayName(adminDisplayName)
                    .companyName(sirket.getAd())
                    .sirketId(sirket.getId())
                    .sirketler(new HashSet<>(Set.of(sirket)))
                    .role("ADMIN")
                    .active(true)
                    .twoFactorEnabled(false)
                    .build();
            kullaniciRepository.save(admin);
            log.warn("İlk kullanıcı oluşturuldu -> Kullanıcı adı: {}, Rol: ADMIN. Varsayılan şifreyi girişten sonra değiştirin!", adminUsername);
        }
    }
}
