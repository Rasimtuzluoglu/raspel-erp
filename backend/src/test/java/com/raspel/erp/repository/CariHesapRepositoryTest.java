package com.raspel.erp.repository;

import com.raspel.erp.entity.CariHesap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CariHesapRepositoryTest {

    @Autowired
    private CariHesapRepository cariHesapRepository;

    @BeforeEach
    void setUp() {
        cariHesapRepository.deleteAll();
    }

    private CariHesap createCariHesap(String ad, BigDecimal bakiye) {
        CariHesap c = new CariHesap();
        c.setAd(ad);
        c.setBakiye(bakiye);
        c.setVergiNumarasi("1234567890");
        c.setTelefon("5551234567");
        return c;
    }

    @Test
    void toplamBakiyeHesapla_returnsSumOfAllBalances() {
        cariHesapRepository.save(createCariHesap("Cari A", BigDecimal.valueOf(1000)));
        cariHesapRepository.save(createCariHesap("Cari B", BigDecimal.valueOf(2000)));
        cariHesapRepository.save(createCariHesap("Cari C", BigDecimal.valueOf(3000)));

        BigDecimal result = cariHesapRepository.toplamBakiyeHesapla();

        assertEquals(0, BigDecimal.valueOf(6000).compareTo(result));
    }

    @Test
    void toplamBakiyeHesapla_returnsZeroWhenNoRecords() {
        BigDecimal result = cariHesapRepository.toplamBakiyeHesapla();

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void findByAdContainingIgnoreCase_returnsMatchingResults() {
        cariHesapRepository.save(createCariHesap("ABC Müşteri", BigDecimal.ZERO));
        cariHesapRepository.save(createCariHesap("XYZ Tedarikçi", BigDecimal.ZERO));
        cariHesapRepository.save(createCariHesap("abc limited", BigDecimal.ZERO));

        List<CariHesap> result = cariHesapRepository.findByAdContainingIgnoreCase("abc");

        assertEquals(2, result.size());
    }

    @Test
    void findByAdContainingIgnoreCase_returnsEmptyWhenNoMatch() {
        cariHesapRepository.save(createCariHesap("Test Cari", BigDecimal.ZERO));

        List<CariHesap> result = cariHesapRepository.findByAdContainingIgnoreCase("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void save_persistsAndGeneratesId() {
        CariHesap saved = cariHesapRepository.save(createCariHesap("Yeni Cari", BigDecimal.valueOf(500)));

        assertNotNull(saved.getId());
        assertEquals("Yeni Cari", saved.getAd());
    }

    @Test
    void findById_returnsEntityWhenExists() {
        CariHesap saved = cariHesapRepository.save(createCariHesap("Aranan Cari", BigDecimal.TEN));

        Optional<CariHesap> found = cariHesapRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Aranan Cari", found.get().getAd());
    }

    @Test
    void findById_returnsEmptyWhenNotExists() {
        Optional<CariHesap> found = cariHesapRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void delete_removesEntity() {
        CariHesap saved = cariHesapRepository.save(createCariHesap("Silinecek Cari", BigDecimal.ZERO));

        cariHesapRepository.deleteById(saved.getId());

        Optional<CariHesap> found = cariHesapRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }
}
