package com.raspel.erp.config;

import com.raspel.erp.entity.Donem;
import com.raspel.erp.entity.Kullanici;
import com.raspel.erp.entity.Sirket;
import com.raspel.erp.repository.DonemRepository;
import com.raspel.erp.repository.KullaniciRepository;
import com.raspel.erp.repository.SirketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final KullaniciRepository kullaniciRepository;
    private final SirketRepository sirketRepository;
    private final DonemRepository donemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (kullaniciRepository.count() > 0) {
            log.info("Veritabaninda mevcut veri var, seed atlaniyor.");
            return;
        }

        log.info("Seed verileri olusturuluyor...");

        Sirket sirket = Sirket.builder()
                .ad("ABC Ön Muhasebe")
                .vergiNo("1234567890")
                .vergiDairesi("Kadıköy VD")
                .adres("İstanbul, Türkiye")
                .telefon("0212 123 45 67")
                .email("info@abconmuhasebe.com")
                .webSite("https://abconmuhasebe.com")
                .aktif(true)
                .build();
        sirket = sirketRepository.save(sirket);
        log.info("Varsayilan sirket olusturuldu: {}", sirket.getAd());

        Sirket sirket2 = Sirket.builder()
                .ad("DEF Ticaret")
                .vergiNo("9876543210")
                .vergiDairesi("Mecidiyeköy VD")
                .adres("İstanbul, Türkiye")
                .telefon("0216 987 65 43")
                .aktif(true)
                .build();
        sirket2 = sirketRepository.save(sirket2);
        log.info("Ikinci sirket olusturuldu: {}", sirket2.getAd());

        Donem donem2024 = Donem.builder()
                .sirketId(sirket.getId())
                .ad("2024 Yılı")
                .baslangic(LocalDate.of(2024, 1, 1))
                .bitis(LocalDate.of(2024, 12, 31))
                .aktif(false)
                .build();
        donemRepository.save(donem2024);

        Donem donem2025 = Donem.builder()
                .sirketId(sirket.getId())
                .ad("2025 Yılı")
                .baslangic(LocalDate.of(2025, 1, 1))
                .bitis(LocalDate.of(2025, 12, 31))
                .aktif(false)
                .build();
        donemRepository.save(donem2025);

        Donem donem2026 = Donem.builder()
                .sirketId(sirket.getId())
                .ad("2026 Yılı")
                .baslangic(LocalDate.of(2026, 1, 1))
                .bitis(LocalDate.of(2026, 12, 31))
                .aktif(true)
                .build();
        donemRepository.save(donem2026);

        Kullanici admin = Kullanici.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .displayName("Ahmet Yılmaz")
                .avatarUrl("https://api.dicebear.com/7.x/initials/svg?seed=AY&backgroundColor=1976d2&textColor=ffffff")
                .companyName("ABC Ön Muhasebe")
                .sirketId(sirket.getId())
                .role("ADMIN")
                .active(true)
                .build();
        kullaniciRepository.save(admin);

        Kullanici muhasebe = Kullanici.builder()
                .username("muhasebe")
                .password(passwordEncoder.encode("123456"))
                .displayName("Ayşe Demir")
                .avatarUrl("https://api.dicebear.com/7.x/initials/svg?seed=AD&backgroundColor=4caf50&textColor=ffffff")
                .companyName("ABC Ön Muhasebe")
                .sirketId(sirket.getId())
                .role("USER")
                .active(true)
                .build();
        kullaniciRepository.save(muhasebe);

        Kullanici ali = Kullanici.builder()
                .username("ali")
                .password(passwordEncoder.encode("123456"))
                .displayName("Ali Kaya")
                .avatarUrl("https://api.dicebear.com/7.x/initials/svg?seed=AK&backgroundColor=ff9800&textColor=ffffff")
                .companyName("DEF Ticaret")
                .sirketId(sirket2.getId())
                .role("USER")
                .active(true)
                .build();
        kullaniciRepository.save(ali);

        Kullanici zeynep = Kullanici.builder()
                .username("zeynep")
                .password(passwordEncoder.encode("123456"))
                .displayName("Zeynep Şahin")
                .avatarUrl("https://api.dicebear.com/7.x/initials/svg?seed=ZS&backgroundColor=9c27b0&textColor=ffffff")
                .companyName("ABC Ön Muhasebe")
                .sirketId(sirket.getId())
                .role("USER")
                .active(true)
                .build();
        kullaniciRepository.save(zeynep);

        log.info("Seed verileri olusturuldu.");
    }
}
