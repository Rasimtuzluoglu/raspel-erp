-- RasPel ERP - Yuk testi veri ureticisi (idempotent)
-- Izole bir "YUK TESTI" sirketi olusturur ve gercekci hacimde veri basar.
-- Yeniden calistirildiginda once eski test verisini siler.
--
-- Kullanim: scripts/generate-test-data.ps1  (psql ile bu dosyayi calistirir)
-- NOT: Demo (RasPel Test) sirketine DOKUNMAZ.

\set ON_ERROR_STOP on

BEGIN;

-- Test sirketi (yoksa olustur)
INSERT INTO sistem.sirket (ad, vergi_no, aktif, olusturma_tarihi, tur)
SELECT 'YUK TESTI', '0000000000', true, now(), 'GAYRIRESMI'
WHERE NOT EXISTS (SELECT 1 FROM sistem.sirket WHERE ad = 'YUK TESTI');

CREATE TEMP TABLE _ts ON COMMIT DROP AS
SELECT id AS sirket_id FROM sistem.sirket WHERE ad = 'YUK TESTI' LIMIT 1;

-- Onceki test verisini temizle (tekrar calistirmada cakisma olmasin)
DELETE FROM fatura.fatura_kalem k
 USING fatura.fatura f, _ts t WHERE k.fatura_id = f.id AND f.sirket_id = t.sirket_id;
DELETE FROM fatura.fatura f USING _ts t WHERE f.sirket_id = t.sirket_id;
DELETE FROM cari.hareket h USING _ts t WHERE h.sirket_id = t.sirket_id;
DELETE FROM stok.stok_hareket sh
 USING stok.stok s, _ts t WHERE sh.stok_id = s.id AND s.sirket_id = t.sirket_id;
DELETE FROM cari.cari_hesap c USING _ts t WHERE c.sirket_id = t.sirket_id;
DELETE FROM stok.stok s USING _ts t WHERE s.sirket_id = t.sirket_id;

-- 1) 5.000 cari
INSERT INTO cari.cari_hesap
    (ad, vergi_numarasi, telefon, email, adres, bakiye, olusturma_tarihi, guncelleme_tarihi,
     sirket_id, tur, il, aktif, odeme_vadesi, version)
SELECT 'Test Cari ' || i,
       '99' || lpad(i::text, 9, '0'),
       '555' || lpad(i::text, 7, '0'),
       'cari' || i || '@test.local',
       'Test Mah. No:' || i || ' Istanbul',
       0, now(), now(), t.sirket_id, 'MUSTERI', 'Istanbul', true, (i % 30), 0
FROM generate_series(1, 5000) i, _ts t;

-- 2) 2.000 stok
INSERT INTO stok.stok
    (stok_kodu, ad, birim, fiyat, miktar, min_miktar, olusturma_tarihi, sirket_id,
     kdv_orani, barkod, satis_fiyati, kategori, raf_no, marka, version)
SELECT 'YSTK-' || lpad(i::text, 6, '0'),
       'Test Urun ' || i,
       'ADET',
       (i % 500) + 10,
       100000,
       50,
       now(), t.sirket_id, 20,
       '869' || lpad(i::text, 10, '0'),
       (i % 500) + 10,
       'KATEGORI-' || (i % 20),
       'RAF-' || (i % 100),
       'MARKA-' || (i % 10),
       0
FROM generate_series(1, 2000) i, _ts t;

-- Yardimci: test sirketinin id araliklari
CREATE TEMP TABLE _aralik ON COMMIT DROP AS
SELECT t.sirket_id,
       (SELECT min(id) FROM cari.cari_hesap WHERE sirket_id = t.sirket_id) AS cari_min,
       (SELECT min(id) FROM stok.stok       WHERE sirket_id = t.sirket_id) AS stok_min
FROM _ts t;

-- 3) 100.000 fatura (SATIS / KESILDI), 260 gunluk dagilim
INSERT INTO fatura.fatura
    (fatura_numarasi, tarih, tur, durum, cari_hesap_id, aciklama,
     ara_toplam, kdv, genel_toplam, olusturma_tarihi, sirket_id,
     genel_iskonto_tutari, odeme_durumu, odenen_tutar, kalan_tutar,
     para_birimi, olusturan_kullanici_adi, version)
SELECT 'YUK-' || lpad(i::text, 8, '0'),
       DATE '2026-01-01' + (i % 260),
       'SATIS', 'KESILDI',
       a.cari_min + (i % 5000),
       'Yuk testi faturasi',
       (i % 1000) + 100,
       ((i % 1000) + 100) * 0.20,
       ((i % 1000) + 100) * 1.20,
       now(), a.sirket_id,
       0, 'ODENMEDI', 0, ((i % 1000) + 100) * 1.20,
       'TRY', 'Yuk Testi', 0
FROM generate_series(1, 100000) i, _aralik a;

-- 4) Fatura kalemleri (fatura basina 3 kalem => 300.000)
INSERT INTO fatura.fatura_kalem
    (fatura_id, aciklama, adet, birim_fiyat, kdv_orani, tutar, stok_id, iskonto_orani, olusturma_tarihi)
SELECT f.id,
       'Kalem ' || k,
       k,
       (f.id % 200) + 5,
       20,
       k * ((f.id % 200) + 5),
       a.stok_min + ((f.id + k) % 2000),
       0,
       now()
FROM fatura.fatura f
JOIN _aralik a ON a.sirket_id = f.sirket_id
CROSS JOIN generate_series(1, 3) k;

-- 5) 150.000 cari hareket
INSERT INTO cari.hareket
    (cari_hesap_id, tur, tutar, hareket_tarihi, aciklama, olusturma_tarihi, sirket_id)
SELECT a.cari_min + (i % 5000),
       (ARRAY['TAHSILAT','ODEME','BORC'])[(i % 3) + 1],
       (i % 900) + 100,
       DATE '2026-01-01' + (i % 260),
       'Yuk testi hareketi',
       now(), a.sirket_id
FROM generate_series(1, 150000) i, _aralik a;

-- 6) 200.000 stok hareket
INSERT INTO stok.stok_hareket
    (stok_id, tur, miktar, hareket_tarihi, aciklama, olusturma_tarihi)
SELECT a.stok_min + (i % 2000),
       (ARRAY['GIRIS','CIKIS'])[(i % 2) + 1],
       (i % 50) + 1,
       DATE '2026-01-01' + (i % 260),
       'Yuk testi stok hareketi',
       now()
FROM generate_series(1, 200000) i, _aralik a;

COMMIT;

-- Planlayici istatistikleri (gercekci sorgu planlari icin)
ANALYZE cari.cari_hesap;
ANALYZE cari.hareket;
ANALYZE stok.stok;
ANALYZE stok.stok_hareket;
ANALYZE fatura.fatura;
ANALYZE fatura.fatura_kalem;

SELECT s.id AS test_sirket_id,
       (SELECT count(*) FROM cari.cari_hesap  WHERE sirket_id = s.id) AS cari,
       (SELECT count(*) FROM stok.stok        WHERE sirket_id = s.id) AS stok,
       (SELECT count(*) FROM fatura.fatura    WHERE sirket_id = s.id) AS fatura,
       (SELECT count(*) FROM cari.hareket     WHERE sirket_id = s.id) AS hareket
FROM sistem.sirket s WHERE s.ad = 'YUK TESTI';
