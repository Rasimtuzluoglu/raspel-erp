package com.raspel.erp.config;

import com.raspel.erp.entity.sistem.Kullanici;
import com.raspel.erp.repository.sistem.KullaniciRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.raspel.erp.entity.sistem.Rol;

/**
 * İlk kurulum: veritabanında hiç kullanıcı yoksa varsayılan ADMIN kullanıcısı oluşturur.
 * Kullanıcı adı/şifre ortam değişkenleriyle özelleştirilebilir (APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IlkKullaniciInitializer implements CommandLineRunner {

    private final KullaniciRepository kullaniciRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.display-name:Yönetici}")
    private String adminDisplayName;

    @Override
    public void run(String... args) {
        if (kullaniciRepository.count() > 0) return;

        Kullanici admin = Kullanici.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .displayName(adminDisplayName)
                .role("ADMIN")
                .active(true)
                .twoFactorEnabled(false)
                .build();
        kullaniciRepository.save(admin);
        log.warn("İlk kullanıcı oluşturuldu -> Kullanıcı adı: {}, Rol: ADMIN. Varsayılan şifreyi girişten sonra değiştirin!", adminUsername);
    }
}