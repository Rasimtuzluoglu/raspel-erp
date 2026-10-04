-- =====================================================================
-- RAZPEK RED TEAM - TEST VERISI (iki tenant'ta AYNI ADLI kayitlar)
-- Amac: cross-tenant sizinti kanitlamak. Ayni ad, farkli sirket_id.
-- Tenant A sirket_id=4, Tenant B sirket_id=99
-- =====================================================================

BEGIN;

-- 1) CARI: her tenant'ta "ZZTEST Ortak Musteri"
INSERT INTO cari.cari_hesap
  (ad, vergi_numarasi, telefon, bakiye, olusturma_tarihi, guncelleme_tarihi, email, adres, sirket_id, tur, aktif, kredi_limiti, odeme_vadesi)
SELECT 'ZZTEST Ortak Musteri', '1000000001', '05550000001', 5000,
       now(), now(), 'ortak@test.local', 'ZZTEST Adresi', 4, 'CARI', true, 100000, 30
WHERE NOT EXISTS (SELECT 1 FROM cari.cari_hesap WHERE ad='ZZTEST Ortak Musteri' AND sirket_id=4);

INSERT INTO cari.cari_hesap
  (ad, vergi_numarasi, telefon, bakiye, olusturma_tarihi, guncelleme_tarihi, email, adres, sirket_id, tur, aktif, kredi_limiti, odeme_vadesi)
SELECT 'ZZTEST Ortak Musteri', '1000000002', '05550000002', 7000,
       now(), now(), 'ortakB@test.local', 'ZZTEST Adresi B', 99, 'CARI', true, 100000, 30
WHERE NOT EXISTS (SELECT 1 FROM cari.cari_hesap WHERE ad='ZZTEST Ortak Musteri' AND sirket_id=99);

-- 2) STOK: her tenant'ta "ZZTEST Beyaz Masa" (miktar 100)
-- NOT: stok.stok tablosunda `aktif` kolonu YOK (V1 semasi)
INSERT INTO stok.stok
  (ad, stok_kodu, birim, fiyat, satis_fiyati, miktar, kdv_orani, sirket_id, kategori, stok_grubu, barkod, olusturma_tarihi)
SELECT 'ZZTEST Beyaz Masa', 'ZZTEST-001', 'ADET', 100.00, 150.00, 100, 20, 4, 'Gida', 'MAMUL', 'ZZTEST0001', now()
WHERE NOT EXISTS (SELECT 1 FROM stok.stok WHERE stok_kodu='ZZTEST-001');

INSERT INTO stok.stok
  (ad, stok_kodu, birim, fiyat, satis_fiyati, miktar, kdv_orani, sirket_id, kategori, stok_grubu, barkod, olusturma_tarihi)
SELECT 'ZZTEST Beyaz Masa', 'ZZTEST-002', 'ADET', 100.00, 150.00, 100, 20, 99, 'Gida', 'MAMUL', 'ZZTEST0002', now()
WHERE NOT EXISTS (SELECT 1 FROM stok.stok WHERE stok_kodu='ZZTEST-002');

-- 3) KASA (harcama testleri icin) - kasa tablosunda sadece ad/bakiye/sirket_id
INSERT INTO muhasebe.kasa (ad, sirket_id, bakiye, olusturma_tarihi)
SELECT 'ZZTEST Kasa A', 4, 100000.00, now()
WHERE NOT EXISTS (SELECT 1 FROM muhasebe.kasa WHERE ad='ZZTEST Kasa A');

INSERT INTO muhasebe.kasa (ad, sirket_id, bakiye, olusturma_tarihi)
SELECT 'ZZTEST Kasa B', 99, 100000.00, now()
WHERE NOT EXISTS (SELECT 1 FROM muhasebe.kasa WHERE ad='ZZTEST Kasa B');

-- 4) BANKA (odeme testleri icin)
INSERT INTO muhasebe.banka (ad, sirket_id, bakiye, olusturma_tarihi, iban)
SELECT 'ZZTEST Banka A', 4, 50000.00, now(), 'TRZZTESTA0001'
WHERE NOT EXISTS (SELECT 1 FROM muhasebe.banka WHERE ad='ZZTEST Banka A');

INSERT INTO muhasebe.banka (ad, sirket_id, bakiye, olusturma_tarihi, iban)
SELECT 'ZZTEST Banka B', 99, 50000.00, now(), 'TRZZTESTB0001'
WHERE NOT EXISTS (SELECT 1 FROM muhasebe.banka WHERE ad='ZZTEST Banka B');

COMMIT;

-- 5) Oluşturulan kayıtların kimliklerini raporla (saldırı komutlarında lazım)
\echo '=== CARI ==='
SELECT id, ad, sirket_id FROM cari.cari_hesap WHERE ad LIKE 'ZZTEST%' ORDER BY sirket_id;
\echo '=== STOK ==='
SELECT id, ad, stok_kodu, miktar, sirket_id FROM stok.stok WHERE stok_kodu LIKE 'ZZTEST%' ORDER BY sirket_id;
\echo '=== KASA ==='
SELECT id, ad, bakiye, sirket_id FROM muhasebe.kasa WHERE ad LIKE 'ZZTEST%' ORDER BY sirket_id;
\echo '=== BANKA ==='
SELECT id, ad, bakiye, sirket_id FROM muhasebe.banka WHERE ad LIKE 'ZZTEST%' ORDER BY sirket_id;