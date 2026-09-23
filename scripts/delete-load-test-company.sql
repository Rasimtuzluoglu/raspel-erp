-- Yük testi firmasını (sirket_id=5, "YUK TESTI") ve tüm verisini siler.
-- Tek seferlik operasyonel temizlik; Flyway migration DEĞİLDİR (ortama özgü).
-- Bağımlılık sırasına göre siler; tek transaction içinde çalışır (hata olursa geri alınır).
-- Kullanım: psql -v ON_ERROR_STOP=1 -f delete-load-test-company.sql

\set ON_ERROR_STOP on

BEGIN;

-- Fatura
DELETE FROM fatura.fatura_kalem
 WHERE fatura_id IN (SELECT id FROM fatura.fatura WHERE sirket_id = 5);
DELETE FROM fatura.fatura_gecmis WHERE sirket_id = 5;
DELETE FROM fatura.fatura        WHERE sirket_id = 5;

-- Stok / maliyet
DELETE FROM stok.stok_hareket
 WHERE stok_id IN (SELECT id FROM stok.stok WHERE sirket_id = 5);
DELETE FROM maliyet.stok_maliyet_hareket WHERE sirket_id = 5;
DELETE FROM stok.stok WHERE sirket_id = 5;

-- Cari
DELETE FROM cari.hareket    WHERE sirket_id = 5;
DELETE FROM cari.cari_hesap WHERE sirket_id = 5;

-- Sistem
DELETE FROM sistem.seri_sayac WHERE sirket_id = 5;
DELETE FROM sistem.audit_log  WHERE sirket_id = 5;

-- Firma kaydı
DELETE FROM sistem.sirket WHERE id = 5;

COMMIT;

-- Doğrulama
SELECT count(*) AS kalan_sirket_5 FROM sistem.sirket WHERE id = 5;
SELECT 'fatura.fatura' AS tbl, count(*) AS kalan FROM fatura.fatura WHERE sirket_id = 5
UNION ALL SELECT 'cari.cari_hesap', count(*) FROM cari.cari_hesap WHERE sirket_id = 5
UNION ALL SELECT 'cari.hareket', count(*) FROM cari.hareket WHERE sirket_id = 5
UNION ALL SELECT 'stok.stok', count(*) FROM stok.stok WHERE sirket_id = 5
UNION ALL SELECT 'sistem.audit_log', count(*) FROM sistem.audit_log WHERE sirket_id = 5;
