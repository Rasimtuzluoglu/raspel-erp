package com.raspel.erp.repository;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.repository.finans.CariHesapRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gercek PostgreSQL uzerinde Flyway migration'lari ve tenant-scoped
 * repository sorgularinin dogrulugunu test eder.
 * Docker yoksa otomatik olarak atlanir (CI'da calisir).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class PostgresEntegrasyonTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("raspelerp")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void veritabaniAyarlari(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private CariHesapRepository cariHesapRepository;

    @Test
    void flywayMigrasyonlariBasarili() {
        assertTrue(postgres.isRunning());
    }

    @Test
    void cariHesap_tenantFiltreliSorguCalisir() {
        cariHesapRepository.save(ornekCari(1L, "Firma A Cari"));
        cariHesapRepository.save(ornekCari(2L, "Firma B Cari"));

        var firmaA = cariHesapRepository.findBySirketId(1L, org.springframework.data.domain.Pageable.unpaged());
        assertEquals(1, firmaA.getContent().size());
        assertEquals("Firma A Cari", firmaA.getContent().get(0).getAd());
    }

    @Test
    void cariHesap_aynıTenantlarKarismaz() {
        cariHesapRepository.save(ornekCari(10L, "Tenant 10 Cari"));
        cariHesapRepository.save(ornekCari(20L, "Tenant 20 Cari"));

        var firma10 = cariHesapRepository.findBySirketId(10L, org.springframework.data.domain.Pageable.unpaged());
        assertEquals(1, firma10.getContent().size());
        assertEquals("Tenant 10 Cari", firma10.getContent().get(0).getAd());
    }

    private CariHesap ornekCari(Long sirketId, String ad) {
        return CariHesap.builder()
                .ad(ad)
                .vergiNumarasi("1234567890")
                .bakiye(BigDecimal.ZERO)
                .sirketId(sirketId)
                .olusturmaTarihi(LocalDateTime.now())
                .guncellemeTarihi(LocalDateTime.now())
                .build();
    }
}
