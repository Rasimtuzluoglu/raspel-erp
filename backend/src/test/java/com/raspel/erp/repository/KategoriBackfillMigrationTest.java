package com.raspel.erp.repository;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V173 backfill'inin GERÇEK PostgreSQL üzerinde doğrulanması.
 *
 * <p>Diğer entegrasyon testinden farkı: migrate önce <b>V172'ye kadar</b>
 * çalıştırılır, eski {@code sistem.gelir_gider_kategori} verisi (kasa
 * hareketlerinin işaret ettiği ID'ler) ve legacy stok/masraf satırları elle
 * eklenir, sonra V173 uygulanır. Böylece:
 * <ul>
 *   <li>ID korunumu (kasa hareketi bağları kopmuyor)</li>
 *   <li>kapsam eşlemesi (GELIR → KASA_GELIR, GIDER → KASA_GIDER)</li>
 *   <li>MASRAF/BUTCE büyük harf normalizasyonu ve kategori_id bağlama</li>
 *   <li>sequence hizalanması</li>
 * </ul>
 * doğrulanır.
 */
@Testcontainers(disabledWithoutDocker = true)
class KategoriBackfillMigrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("raspelerp")
            .withUsername("postgres")
            .withPassword("postgres");

    private Flyway flyway(String target) {
        var config = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .placeholders(Map.of("app-db-password", ""));
        if (target != null) {
            config = config.target(MigrationVersion.fromVersion(target));
        }
        return config.load();
    }

    private Connection baglanti() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @Test
    void backfillIdleriKorurKapsamEslerVeSequenceHizalanir() throws Exception {
        // 1) V172'ye kadar migrate et (V173 hariç).
        flyway("172").migrate();

        long sirketId;
        long gelirKategoriId;
        long giderKategoriId;
        try (Connection c = baglanti()) {
            sirketId = tekDeger(c,
                    "INSERT INTO sistem.sirket (ad, olusturma_tarihi) VALUES ('Backfill Firma', NOW()) RETURNING id");

            gelirKategoriId = tekDeger(c,
                    "INSERT INTO sistem.gelir_gider_kategori (ad, tur, sirket_id, olusturma_tarihi) " +
                            "VALUES ('Satış', 'GELIR', " + sirketId + ", NOW()) RETURNING id");
            giderKategoriId = tekDeger(c,
                    "INSERT INTO sistem.gelir_gider_kategori (ad, tur, sirket_id, olusturma_tarihi) " +
                            "VALUES ('Maaş', 'GIDER', " + sirketId + ", NOW()) RETURNING id");

            // Aynı ID'lerle kasa hareketleri (eski tabloda FK yok).
            tekDeger(c, "INSERT INTO muhasebe.kasa (ad, bakiye, sirket_id, olusturma_tarihi) " +
                    "VALUES ('Merkez', 0, " + sirketId + ", NOW()) RETURNING id");
            long kasaId = tekDeger(c, "SELECT id FROM muhasebe.kasa WHERE sirket_id = " + sirketId + " LIMIT 1");
            tekDeger(c, "INSERT INTO muhasebe.kasa_hareket " +
                    "(kasa_id, tur, tutar, hareket_tarihi, kategori_id, olusturma_tarihi) VALUES (" +
                    kasaId + ", 'GELIR', 100, CURRENT_DATE, " + gelirKategoriId + ", NOW()) RETURNING id");

            // Legacy string kolonlar: stok (STOK), masraf (MASRAF, küçük harf), bütçe (BUTCE).
            tekDeger(c, "INSERT INTO stok.stok (stok_kodu, ad, fiyat, miktar, kategori, sirket_id, olusturma_tarihi) " +
                    "VALUES ('BF-1', 'Backfill Ürün', 10, 1, 'Elektronik', " + sirketId + ", NOW()) RETURNING id");
            tekDeger(c, "INSERT INTO ticaret.iskonto_kurali (sirket_id, ad, kategori, iskonto_orani, olusturma_tarihi) " +
                    "VALUES (" + sirketId + ", 'BF Kural', 'elektronik', 10, NOW()) RETURNING id");
            tekDeger(c, "INSERT INTO finans.masraf (tarih, tutar, kategori, sirket_id, olusturma_tarihi) " +
                    "VALUES (CURRENT_DATE, 50, ' kira ', " + sirketId + ", NOW()) RETURNING id");
            tekDeger(c, "INSERT INTO finans.butce (ad, yil, ay, tutar, tur, kategori, sirket_id, olusturma_tarihi) " +
                    "VALUES ('BF Bütçe', 2026, 1, 1000, 'GIDER', 'kira', " + sirketId + ", NOW()) RETURNING id");
        }

        // 2) V173'ü uygula.
        flyway(null).migrate();

        try (Connection c = baglanti()) {
            // Eski tablo düşürüldü.
            assertEquals("YOK", metin(c, "SELECT COALESCE(to_regclass('sistem.gelir_gider_kategori')::text, 'YOK')"));

            // ID korunumu + kapsam eşlemesi.
            assertEquals("KASA_GELIR", metin(c,
                    "SELECT kapsam FROM sistem.kategori WHERE id = " + gelirKategoriId));
            assertEquals("KASA_GIDER", metin(c,
                    "SELECT kapsam FROM sistem.kategori WHERE id = " + giderKategoriId));
            assertEquals("Satış", metin(c,
                    "SELECT ad FROM sistem.kategori WHERE id = " + gelirKategoriId));

            // Kasa hareketinin kategori bağı hâlâ geçerli.
            assertEquals("Satış", metin(c,
                    "SELECT k.ad FROM muhasebe.kasa_hareket kh " +
                            "JOIN sistem.kategori k ON k.id = kh.kategori_id " +
                            "WHERE kh.kategori_id = " + gelirKategoriId));
            assertEquals(0L, sayi(c,
                    "SELECT count(*) FROM muhasebe.kasa_hareket kh " +
                            "WHERE kh.kategori_id IS NOT NULL AND NOT EXISTS " +
                            "(SELECT 1 FROM sistem.kategori k WHERE k.id = kh.kategori_id)"));

            // Legacy adlar registry'ye taşındı; MASRAF/BUTCE büyük harf.
            assertEquals(1L, sayi(c,
                    "SELECT count(*) FROM sistem.kategori WHERE sirket_id = " + sirketId +
                            " AND kapsam = 'STOK' AND lower(ad) = 'elektronik'"));
            // STOK aynı ad (stok + iskonto) tek satıra bağlanır.
            assertEquals(1L, sayi(c,
                    "SELECT count(*) FROM sistem.kategori WHERE sirket_id = " + sirketId + " AND kapsam = 'STOK'"));
            assertEquals("KIRA", metin(c,
                    "SELECT ad FROM sistem.kategori WHERE sirket_id = " + sirketId + " AND kapsam = 'MASRAF'"));
            assertEquals("KIRA", metin(c,
                    "SELECT ad FROM sistem.kategori WHERE sirket_id = " + sirketId + " AND kapsam = 'BUTCE'"));

            // Legacy kolonlar normalize edildi ve kategori_id bağlandı.
            assertEquals("KIRA", metin(c,
                    "SELECT kategori FROM finans.masraf WHERE sirket_id = " + sirketId));
            assertEquals("KIRA", metin(c,
                    "SELECT kategori FROM finans.butce WHERE sirket_id = " + sirketId));

            assertEquals(0L, sayi(c,
                    "SELECT count(*) FROM stok.stok WHERE sirket_id = " + sirketId + " AND kategori_id IS NULL"));
            assertEquals(0L, sayi(c,
                    "SELECT count(*) FROM ticaret.iskonto_kurali WHERE sirket_id = " + sirketId + " AND kategori_id IS NULL"));
            assertEquals(0L, sayi(c,
                    "SELECT count(*) FROM finans.masraf WHERE sirket_id = " + sirketId + " AND kategori_id IS NULL"));
            assertEquals(0L, sayi(c,
                    "SELECT count(*) FROM finans.butce WHERE sirket_id = " + sirketId + " AND kategori_id IS NULL"));

            // Sequence hizalandı: yeni kayıt max(id)+1'den devam eder.
            long maxId = tekDeger(c, "SELECT MAX(id) FROM sistem.kategori");
            long yeniId = tekDeger(c, "INSERT INTO sistem.kategori (sirket_id, kapsam, ad) VALUES (" +
                    sirketId + ", 'STOK', 'YENI') RETURNING id");
            assertTrue(yeniId > maxId, "Yeni id max(id)'den büyük olmalı: " + yeniId + " > " + maxId);
        }
    }

    private long tekDeger(Connection c, String sql) throws Exception {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next(), "Sonuç yok: " + sql);
            return rs.getLong(1);
        }
    }

    private long sayi(Connection c, String sql) throws Exception {
        return tekDeger(c, sql);
    }

    private String metin(Connection c, String sql) throws Exception {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            assertTrue(rs.next(), "Sonuç yok: " + sql);
            return rs.getString(1);
        }
    }
}
