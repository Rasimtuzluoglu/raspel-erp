package com.raspel.erp.repository;

import com.raspel.erp.entity.Kullanici;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KullaniciRepositoryTest {

    @Autowired
    private KullaniciRepository kullaniciRepository;

    @BeforeEach
    void setUp() {
        kullaniciRepository.deleteAll();
    }

    private Kullanici createKullanici(String username) {
        Kullanici k = new Kullanici();
        k.setUsername(username);
        k.setPassword("encodedPass");
        k.setDisplayName("Test User");
        k.setRole("USER");
        k.setActive(true);
        return k;
    }

    @Test
    void findByUsername_returnsUserWhenExists() {
        kullaniciRepository.save(createKullanici("testuser"));

        Optional<Kullanici> found = kullaniciRepository.findByUsername("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    void findByUsername_returnsEmptyWhenNotExists() {
        Optional<Kullanici> found = kullaniciRepository.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void findByUsername_isCaseSensitive() {
        kullaniciRepository.save(createKullanici("AdminUser"));

        Optional<Kullanici> found = kullaniciRepository.findByUsername("adminuser");

        assertFalse(found.isPresent());
    }

    @Test
    void save_persistsAndGeneratesId() {
        Kullanici saved = kullaniciRepository.save(createKullanici("newuser"));

        assertNotNull(saved.getId());
        assertEquals("newuser", saved.getUsername());
    }

    @Test
    void save_throwsOnDuplicateUsername() {
        kullaniciRepository.save(createKullanici("uniqueuser"));
        kullaniciRepository.flush();

        Kullanici duplicate = createKullanici("uniqueuser");

        assertThrows(Exception.class, () -> {
            kullaniciRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    void findAll_returnsAllUsers() {
        kullaniciRepository.save(createKullanici("user1"));
        kullaniciRepository.save(createKullanici("user2"));
        kullaniciRepository.save(createKullanici("user3"));

        var all = kullaniciRepository.findAll();

        assertEquals(3, all.size());
    }

    @Test
    void delete_removesUser() {
        Kullanici saved = kullaniciRepository.save(createKullanici("deletable"));

        kullaniciRepository.deleteById(saved.getId());

        Optional<Kullanici> found = kullaniciRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void existsById_returnsCorrectBoolean() {
        Kullanici saved = kullaniciRepository.save(createKullanici("existsuser"));

        assertTrue(kullaniciRepository.existsById(saved.getId()));
        assertFalse(kullaniciRepository.existsById(999L));
    }
}
