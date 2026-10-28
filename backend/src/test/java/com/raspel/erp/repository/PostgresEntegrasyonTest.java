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
import com.raspel.erp.repository.ticaret.SiparisRepository;
import com.raspel.erp.repository.ik.MaasBordroRepository;
import com.raspel.erp.repository.sistem.AdresDefteriRepository;
import com.raspel.erp.repository.sistem.SifreKasaRepository;
import com.raspel.erp.repository.sistem.KitaplikRepository;
import com.raspel.erp.entity.sistem.AdresDefteri;
import com.raspel.erp.entity.sistem.SifreKasa;
import com.raspel.erp.entity.sistem.Kitaplik;
import com.raspel.erp.util.AramaTemizleyici;
import com.raspel.erp.repository.sistem.SirketRepository;
import com.raspel.erp.entity.sistem.Sirket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
        // V123 least-privilege rol olusturma migrasyonu placeholder'i (testte bos).
        registry.add("spring.flyway.placeholders.app-db-password", () -> "");
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
    @Autowired
    private SirketRepository sirketRepository;
    @Autowired
    private AdresDefteriRepository adresDefteriRepository;
    @Autowired
    private SiparisRepository siparisRepository;
    @Autowired
    private MaasBordroRepository maasBordroRepository;
    @Autowired
    private SifreKasaRepository sifreKasaRepository;
    @Autowired
    private KitaplikRepository kitaplikRepository;
    @Autowired
    private JdbcTemplate jdbc;

    /** Tenant FK nedeniyle cari/stok kaydetmeden once sirket satiri gerekir. */
    private Long sirketOlustur(String ad) {
        return sirketRepository.save(Sirket.builder().ad(ad).aktif(true).build()).getId();
    }

    @Test
    void flywayMigrasyonlariBasarili() {
        assertTrue(postgres.isRunning());
    }

    /** V172: birleşik onay karar tablosu, indeksleri ve varsayılanları. */
    @Test
    void v172OnayKararTablosuVeIndeksleriOlusur() {
        Integer kolonSayisi = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns " +
                        "WHERE table_schema = 'sistem' AND table_name = 'onay_karar'",
                Integer.class);
        assertEquals(11, kolonSayisi);

        Integer indeksSayisi = jdbc.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE schemaname = 'sistem' AND tablename = 'onay_karar' " +
                        "AND indexname IN ('idx_onay_karar_sirket_tarih', 'idx_onay_karar_tur_kaynak')",
                Integer.class);
        assertEquals(2, indeksSayisi);

        jdbc.update("DELETE FROM sistem.onay_karar WHERE sirket_id = 987654");
        jdbc.update("INSERT INTO sistem.onay_karar (sirket_id, tur, kaynak_id, karar, gerekce) " +
                        "VALUES (?, ?, ?, ?, ?)",
                987654L, "IZIN", 5L, "ONAY", "entegrasyon testi");
        Integer kayitSayisi = jdbc.queryForObject(
                "SELECT count(*) FROM sistem.onay_karar WHERE sirket_id = 987654", Integer.class);
        assertEquals(1, kayitSayisi);

        // olusturma_tarihi varsayılanı (NOW()) uygulanmış olmalı.
        Integer tarihDolu = jdbc.queryForObject(
                "SELECT count(*) FROM sistem.onay_karar WHERE sirket_id = 987654 " +
                        "AND olusturma_tarihi IS NOT NULL", Integer.class);
        assertEquals(1, tarihDolu);
        jdbc.update("DELETE FROM sistem.onay_karar WHERE sirket_id = 987654");
    }

    @Test
    void veriButunluguFkVeNotNullKisitlariVar() {
        Integer fkSayisi = jdbc.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE contype = 'f' AND conname IN (" +
                        "'fk_fatura_cari','fk_fatura_sirket','fk_banka_hareketi_kaynak_fatura'," +
                        "'fk_kasa_hareket_fatura','fk_taksit_fatura','fk_siparis_cari','fk_irsaliye_cari'," +
                        "'fk_cari_hesap_sirket')", Integer.class);
        assertEquals(8, fkSayisi);

        Integer notNullSayisi = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE is_nullable = 'NO' AND (" +
                        "(table_schema='fatura' AND table_name='fatura' AND column_name='sirket_id') OR " +
                        "(table_schema='stok' AND table_name='stok' AND column_name='sirket_id') OR " +
                        "(table_schema='cari' AND table_name='cari_hesap' AND column_name='sirket_id'))",
                Integer.class);
        assertEquals(3, notNullSayisi);

        // V119 sonrasi dogrulanmamis (NOT VALID) FK kalmamali.
        Integer notValidSayisi = jdbc.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE contype = 'f' AND NOT convalidated", Integer.class);
        assertEquals(0, notValidSayisi);

        // V120 + V121: iyimser kilitleme version kolonlari (20 tablo).
        Integer versionSayisi = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE column_name = 'version' AND (" +
                        "(table_schema='muhasebe' AND table_name='kasa_hareket') OR " +
                        "(table_schema='finans' AND table_name='banka_hareketi') OR " +
                        "(table_schema='cari' AND table_name='hareket') OR " +
                        "(table_schema='ticaret' AND table_name='iade') OR " +
                        "(table_schema='siparis' AND table_name='siparis') OR " +
                        "(table_schema='muhasebe' AND table_name='irsaliye') OR " +
                        "(table_schema='finans' AND table_name='taksit') OR " +
                        "(table_schema='stok' AND table_name='stok_hareket') OR " +
                        "(table_schema='fatura' AND table_name='fatura_kalem') OR " +
                        "(table_schema='ticaret' AND table_name='iade_kalem') OR " +
                        "(table_schema='siparis' AND table_name='siparis_kalem') OR " +
                        "(table_schema='muhasebe' AND table_name='irsaliye_kalem') OR " +
                        "(table_schema='envanter' AND table_name='stok_seri') OR " +
                        "(table_schema='envanter' AND table_name='stok_sayim') OR " +
                        "(table_schema='maliyet' AND table_name='stok_maliyet_hareket') OR " +
                        "(table_schema='stok' AND table_name='stok_fiyat') OR " +
                        "(table_schema='cari' AND table_name='cari_fiyat') OR " +
                        "(table_schema='muhasebe' AND table_name='cek_senet') OR " +
                        "(table_schema='finans' AND table_name='masraf') OR " +
                        "(table_schema='finans' AND table_name='butce'))", Integer.class);
        assertEquals(20, versionSayisi);

        // V122: sirket_id tasiyan tablolarin tamaminda tenant FK olmali.
        Integer tenantFkSayisi = jdbc.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE contype = 'f' AND conname LIKE 'fk_%_sirket'",
                Integer.class);
        assertTrue(tenantFkSayisi >= 60, "tenant FK sayisi beklenenden az: " + tenantFkSayisi);

        // V124: satinalma tablolarinda sirket_id NOT NULL.
        Integer satinalmaNotNull = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE is_nullable='NO' AND (" +
                        "(table_schema='satinalma' AND table_name='satinalma_talep' AND column_name='sirket_id') OR " +
                        "(table_schema='satinalma' AND table_name='satinalma_siparis' AND column_name='sirket_id'))",
                Integer.class);
        assertEquals(2, satinalmaNotNull);
    }

    @Test
    void faturaAra_nullTarihParametreleriyleCalisir() {
        Long sirket = sirketOlustur("Ara Firma");
        CariHesap cari = cariHesapRepository.save(ornekCari(sirket, "Ara Cari"));
        faturaRepository.save(Fatura.builder()
                .faturaNumarasi("ARA-1").tarih(LocalDate.of(2026, 2, 1))
                .tur(Fatura.FaturaTur.SATIS).durum(Fatura.FaturaDurum.KESILDI)
                .cariHesap(cari)
                .araToplam(BigDecimal.valueOf(100)).kdv(BigDecimal.valueOf(20))
                .genelToplam(BigDecimal.valueOf(100)).kalanTutar(BigDecimal.valueOf(100))
                .sirketId(sirket).build());

        // Regresyon: null bas/bit parametreleri Postgres'te 42P18
        // ("could not determine data type of parameter") veriyordu.
        var sayfa = faturaRepository.ara(sirket, null, null, null,
                Fatura.FaturaTur.SATIS, null, null, null, null,
                LocalDate.of(2026, 6, 1), org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, sayfa.getTotalElements());

        var sayfaFiltreli = faturaRepository.ara(sirket, null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                Fatura.FaturaTur.SATIS, Fatura.FaturaDurum.KESILDI, null, Boolean.FALSE, null,
                LocalDate.of(2026, 6, 1), org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, sayfaFiltreli.getTotalElements());
    }

    @Test
    void cariHesap_tenantFiltreliSorguCalisir() {
        Long firmaAId = sirketOlustur("Firma A");
        Long firmaBId = sirketOlustur("Firma B");
        cariHesapRepository.save(ornekCari(firmaAId, "Firma A Cari"));
        cariHesapRepository.save(ornekCari(firmaBId, "Firma B Cari"));

        var firmaA = cariHesapRepository.findBySirketId(firmaAId, org.springframework.data.domain.Pageable.unpaged());
        assertEquals(1, firmaA.getContent().size());
        assertEquals("Firma A Cari", firmaA.getContent().get(0).getAd());
    }

    @Test
    void cariHesap_aynıTenantlarKarismaz() {
        Long tenant10 = sirketOlustur("Tenant 10");
        Long tenant20 = sirketOlustur("Tenant 20");
        cariHesapRepository.save(ornekCari(tenant10, "Tenant 10 Cari"));
        cariHesapRepository.save(ornekCari(tenant20, "Tenant 20 Cari"));

        var firma10 = cariHesapRepository.findBySirketId(tenant10, org.springframework.data.domain.Pageable.unpaged());
        assertEquals(1, firma10.getContent().size());
        assertEquals("Tenant 10 Cari", firma10.getContent().get(0).getAd());
    }

    private CariHesap ornekCari(Long sirketId, String ad) {
        // uk_cari_vergi_no_sirket benzersiz kisiti: ayni sirkette ayni vergi no olamaz.
        // sirket + ad kombinasyonundan deterministik ve benzersiz bir vergi no uret.
        String vergiNo = String.format("%010d", Math.abs((sirketId * 31L + ad.hashCode()) % 10_000_000_000L));
        return CariHesap.builder()
                .ad(ad)
                .vergiNumarasi(vergiNo)
                .bakiye(BigDecimal.ZERO)
                .sirketId(sirketId)
                .olusturmaTarihi(LocalDateTime.now())
                .guncellemeTarihi(LocalDateTime.now())
                .build();
    }

    // ---------- Ürün analizi repository sorguları (V82 index'leri ile) ----------

    @Test
    void stokAnalizSorgulariCalisir() {
        Long sirket = sirketOlustur("Analiz Firma");
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
                .aciklama("Alış kalemi").adet(java.math.BigDecimal.valueOf(10))
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
                .aciklama("Satış kalemi").adet(java.math.BigDecimal.valueOf(5))
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

    // ------------------------------------------------------------------
    // LIKE arama regresyonu (Postgres'e OZGU)
    //
    // DENETIM BULGUSU: `(:q IS NULL OR LOWER(col) LIKE LOWER(CONCAT('%', :q, '%')))`
    // kalibi PostgreSQL'de "function lower(bytea) does not exist" (SQLState 42883)
    // veriyordu; parametrenin tipi cozulemeyip bytea'ya dusuyordu ve bu q NULL
    // olsa bile olusuyordu. H2 bu kalibi tolere ettigi icin tum H2 testleri yesil
    // kaliyordu. Duzeltme: `LOWER(col) LIKE :desen ESCAPE '\\'` + deseni
    // `AramaTemizleyici.like(q)` ile uretmek.
    //
    // Bu testler GERCEK PostgreSQL uzerinde calisir; hata satir sayisindan
    // bagimsiz (prepare asamasinda) olustugu icin veri eklemeye gerek yoktur.
    // ------------------------------------------------------------------

    @Test
    void adresDefteri_filtreli_nullVeDoluAramaCalisir() {
        Long sirket = sirketOlustur("Adres Arama Firma");
        adresDefteriRepository.save(AdresDefteri.builder()
                .sirketId(sirket).ad("Ali Veli Elektrik").build());

        // null arama da (bytea hatasini tetikleyen yol) calismali
        var bos = adresDefteriRepository.filtreli(sirket, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, bos.getTotalElements());

        var dolu = adresDefteriRepository.filtreli(sirket, AramaTemizleyici.like("ali"), null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, dolu.getTotalElements());
    }

    @Test
    void siparis_filtreliGetir_nullVeDoluAramaCalisir() {
        Long sirket = sirketOlustur("Siparis Arama Firma");
        // Veri eklemeye gerek yok; hata prepare asamasinda olusur.
        assertDoesNotThrow(() -> siparisRepository.filtreliGetir(sirket, null, null, null, false, false,
                java.time.LocalDate.now(), org.springframework.data.domain.PageRequest.of(0, 10)));
        assertDoesNotThrow(() -> siparisRepository.filtreliGetir(sirket, AramaTemizleyici.like("abc"), null, null, false, false,
                java.time.LocalDate.now(), org.springframework.data.domain.PageRequest.of(0, 10)));
    }

    @Test
    void maasBordro_filtreliVeOzet_nullVeDoluAramaCalisir() {
        Long sirket = sirketOlustur("Bordro Arama Firma");
        assertDoesNotThrow(() -> maasBordroRepository.filtreliGetir(sirket, null, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10)));
        assertDoesNotThrow(() -> maasBordroRepository.filtreliGetir(sirket, null, null, null, null,
                AramaTemizleyici.like("ayse"), org.springframework.data.domain.PageRequest.of(0, 10)));
        assertDoesNotThrow(() -> maasBordroRepository.ozet(sirket, null, null, null, null, null));
        assertDoesNotThrow(() -> maasBordroRepository.ozet(sirket, null, null, null, null,
                AramaTemizleyici.like("ayse")));
    }

    @Test
    void sifreKasa_filtreli_nullVeDoluAramaCalisir() {
        Long sirket = sirketOlustur("Kasa Arama Firma");
        sifreKasaRepository.save(SifreKasa.builder()
                .sirketId(sirket).kapsam("GLOBAL").sifreGorunurlugu("SAHIS")
                .baslik("Netsis Muhasebe").sifreCipher("x").aktif(true).build());

        // Kasanin Sirket sekmesi bu sorguyu her zaman calistirir; eskiden 500 idi.
        var bos = sifreKasaRepository.filtreli(sirket, "GLOBAL", true, null, null);
        assertEquals(1, bos.size());

        var dolu = sifreKasaRepository.filtreli(sirket, "GLOBAL", true, null, AramaTemizleyici.like("net"));
        assertEquals(1, dolu.size());

        var eslesmeyen = sifreKasaRepository.filtreli(sirket, "GLOBAL", true, null, AramaTemizleyici.like("zzz"));
        assertEquals(0, eslesmeyen.size());
    }

    @Test
    void kitaplik_gorunurlukVeAramaCalisir() {
        Long sirket = sirketOlustur("Kitaplik Firma");
        // KISISEL (sahip=10), GLOBAL+TUMU, GLOBAL+SAHIS
        kitaplikRepository.save(Kitaplik.builder().sirketId(sirket).kullaniciId(10L)
                .tur("YER_IMI").kapsam("KISISEL").gorunurluk("SAHIS")
                .baslik("Netsis Panel").url("https://netsis.local").aktif(true).build());
        kitaplikRepository.save(Kitaplik.builder().sirketId(sirket).kullaniciId(null)
                .tur("SNIPPET").kapsam("GLOBAL").gorunurluk("TUMU")
                .baslik("Ortak Snippet").icerik("echo merhaba").aktif(true).build());
        kitaplikRepository.save(Kitaplik.builder().sirketId(sirket).kullaniciId(null)
                .tur("SNIPPET").kapsam("GLOBAL").gorunurluk("SAHIS")
                .baslik("Yonetici Notu").icerik("gizli").aktif(true).build());

        // Normal kullanici: kendi KISISEL + GLOBAL+TUMU = 2 (GLOBAL+SAHIS gorunmez)
        var kullanici = kitaplikRepository.gorunurKayitlar(sirket, 10L, false, null, true, null, null);
        assertEquals(2, kullanici.size());
        assertTrue(kullanici.stream().noneMatch(k -> "Yonetici Notu".equals(k.getBaslik())),
                "GLOBAL+SAHIS normal kullaniciya gorunmemeli");

        // ADMIN: GLOBAL+TUMU + GLOBAL+SAHIS = 2 (baskasinin KISISEL kaydini GORMEZ)
        var admin = kitaplikRepository.gorunurKayitlar(sirket, 99L, true, null, true, null, null);
        assertEquals(2, admin.size());
        assertTrue(admin.stream().anyMatch(k -> "Yonetici Notu".equals(k.getBaslik())),
                "ADMIN GLOBAL+SAHIS gormeli");
        assertTrue(admin.stream().noneMatch(k -> "Netsis Panel".equals(k.getBaslik())),
                "ADMIN baskasinin kisisel kaydini gormemeli");

        // Arama (LIKE deseni) Postgres'te calisir
        var arama = kitaplikRepository.gorunurKayitlar(sirket, 10L, false, null, true, null,
                AramaTemizleyici.like("ortak"));
        assertEquals(1, arama.size());
        assertEquals("Ortak Snippet", arama.get(0).getBaslik());

        // Tur filtresi: user10 icin SNIPPET -> GLOBAL+TUMU snippet = 1
        var snippetler = kitaplikRepository.gorunurKayitlar(sirket, 10L, false, "SNIPPET", true, null, null);
        assertEquals(1, snippetler.size());
    }
}
