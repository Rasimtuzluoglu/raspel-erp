package com.raspel.erp;

import com.raspel.erp.entity.Kullanici;
import com.raspel.erp.repository.KullaniciRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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
        List<Kullanici> all = kullaniciRepository.findAll();
        assertFalse(all.isEmpty(), "Seed data should have at least one user");
    }

    @Test
    void testSifreKodlandi() {
        Kullanici admin = kullaniciRepository.findByUsername("admin").orElse(null);
        assertNotNull(admin);
        assertTrue(passwordEncoder.matches("admin123", admin.getPassword()),
                "Password should match encoded password");
    }
}
