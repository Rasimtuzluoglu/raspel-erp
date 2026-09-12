package com.raspel.erp.repository;

import com.raspel.erp.entity.finans.CariHesap;
import com.raspel.erp.entity.envanter.Stok;
import com.raspel.erp.entity.ticaret.Fatura;
import com.raspel.erp.entity.ticaret.FaturaKalem;
import com.raspel.erp.entity.ticaret.Iade;
import com.raspel.erp.entity.ticaret.IadeKalem;
import com.raspel.erp.repository.envanter.StokRepository;
import com.raspel.erp.repository.finans.CariHesapRepository;
import com.raspel.erp.repository.ticaret.FaturaKalemRepository;
import com.raspel.erp.repository.ticaret.FaturaRepository;
import com.raspel.erp.repository.ticaret.IadeKalemRepository;
import com.raspel.erp.repository.ticaret.IadeRepository;
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
import java.time.LocalDate;
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
        // Gercek PostgreSQL uzerinde tablolari Flyway olusturur (H2 profili flyway'i kapatir)
        registry.add("spring.flyway.enabled", () -> "true");
        // H2 icin yazilmis schema.sql PostgreSQL'de calismaz; devre disi birakilir
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired
    private CariHesapRepository cariHesapRepository;
    @Autowired
    private StokRepository stokRepository;
    @Autowired
    private FaturaRepository faturaRepository;
    @Autowired
    private FaturaKalemRepository faturaKalemRepository;
    @Autowired
    private IadeRepository iadeRepository;
    @Autowired
    private IadeKalemRepository iadeKalemRepository;

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

    // ---------- Ürün analizi repository sorguları (V82 index'leri ile) ----------

    @Test
    void stokAnalizSorgulariCalisir() {
        Long sirket = 77L;
        CariHesap tedarikci = cariHesapRepository.save(ornekCari(sirket, "Tedarikçi Test"));
        CariHesap musteri = cariHesapRepository.save(ornekCari(sirket, "Müşteri Test"));
        Stok stok = stokRepository.save(Stok.builder()
                .stokKodu("ANZ-777").ad("Analiz Ürünü").birim("ADET")
                .fiyat(BigDecimal.valueOf(100)).miktar(BigDecimal.valueOf(50))
                .sirketId(sirket).build());

        Fatura alis = Fatura.builder()
                .faturaNumarasi("ANZ-A-777").tarih(LocalDate.of(2026, 1, 10))
                .tur(Fatura.FaturaTur.ALIS).durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(tedarikci)
                .araToplam(BigDecimal.valueOf(1000)).kdv(BigDecimal.valueOf(200))
                .genelToplam(BigDecimal.valueOf(1200)).sirketId(sirket).build();
        alis.getKalemler().add(FaturaKalem.builder().fatura(alis)
                .aciklama("Alış kalemi").adet(10)
                .birimFiyat(BigDecimal.valueOf(100))
                .kdvOrani(BigDecimal.valueOf(20)).iskontoOrani(BigDecimal.TEN)
                .tutar(BigDecimal.valueOf(990)).stokId(stok.getId())
                .build());
        faturaRepository.save(alis);

        Fatura satis = Fatura.builder()
                .faturaNumarasi("ANZ-S-777").tarih(LocalDate.of(2026, 3, 5))
                .tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(musteri)
                .araToplam(BigDecimal.valueOf(1500)).kdv(BigDecimal.valueOf(300))
                .genelToplam(BigDecimal.valueOf(1800)).sirketId(sirket).build();
        satis.getKalemler().add(FaturaKalem.builder().fatura(satis)
                .aciklama("Satış kalemi").adet(5)
                .birimFiyat(BigDecimal.valueOf(300))
                .kdvOrani(BigDecimal.valueOf(20)).iskontoOrani(BigDecimal.ZERO)
                .tutar(BigDecimal.valueOf(1800)).stokId(stok.getId())
                .build());
        faturaRepository.save(satis);

        Iade iade = iadeRepository.save(Iade.builder()
                .faturaId(alis.getId()).tur("ALIS").tarih(LocalDate.of(2026, 1, 25))
                .tutar(BigDecimal.valueOf(240)).durum("TAMAMLANDI").sirketId(sirket).build());
        iadeKalemRepository.save(IadeKalem.builder()
                .iadeId(iade.getId()).stokId(stok.getId())
                .miktar(new BigDecimal("2")).birimFiyat(BigDecimal.valueOf(100))
                .kdvOrani(BigDecimal.valueOf(20)).tutar(BigDecimal.valueOf(240)).build());

        var alislar = faturaKalemRepository.analizSatirlari(
                stok.getId(), sirket, Fatura.FaturaTur.ALIS, Fatura.FaturaDurum.KESILDI,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(1, alislar.size());
        assertEquals(Integer.valueOf(10), alislar.get(0).getAdet());
        assertEquals("Tedarikçi Test", alislar.get(0).getCariHesapAd());
        assertEquals("ANZ-A-777", alislar.get(0).getFaturaNumarasi());

        var satislar = faturaKalemRepository.analizSatirlari(
                stok.getId(), sirket, Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(1, satislar.size());
        assertEquals("Müşteri Test", satislar.get(0).getCariHesapAd());

        var iadeler = iadeKalemRepository.analizSatirlari(
                stok.getId(), sirket, "ALIS", "TAMAMLANDI",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(1, iadeler.size());
        assertEquals(0, new BigDecimal("2").compareTo(iadeler.get(0).getMiktar()));
        // İade'nin cari bilgisi bağlantılı fatura üzerinden gelir (LEFT JOIN)
        assertEquals("Tedarikçi Test", iadeler.get(0).getCariHesapAd());
    }
}
