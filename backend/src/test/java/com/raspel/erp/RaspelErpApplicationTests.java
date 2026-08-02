package com.raspel.erp;

import com.raspel.erp.entity.Kullanici;
import com.raspel.erp.repository.KullaniciRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RaspelErpApplicationTests {

    @Autowired
    private KullaniciRepository kullaniciRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
        assertNotNull(kullaniciRepository);
    }

    @Test
    void testKullaniciOlusturma() {
        Kullanici kullanici = Kullanici.builder()
                .username("test_" + System.currentTimeMillis())
                .password("sifre")
                .displayName("Test Kullanıcı")
                .role("USER")
                .active(true)
                .build();
        Kullanici kayitli = kullaniciRepository.save(kullanici);
        assertNotNull(kayitli.getId());
        assertTrue(kullaniciRepository.findById(kayitli.getId()).isPresent());
        kullaniciRepository.delete(kayitli);
    }

    @Test
    void testSifreKodlandi() {
        String sifre = "gizliSifre123";
        Kullanici kullanici = Kullanici.builder()
                .username("sifre_test_" + System.currentTimeMillis())
                .password(passwordEncoder.encode(sifre))
                .displayName("Şifre Test")
                .role("USER")
                .active(true)
                .build();
        Kullanici kayitli = kullaniciRepository.save(kullanici);
        assertTrue(passwordEncoder.matches(sifre, kayitli.getPassword()),
                "Password should match encoded password");
        assertFalse(passwordEncoder.matches("yanlisSifre", kayitli.getPassword()));
        kullaniciRepository.delete(kayitli);
    }
}
