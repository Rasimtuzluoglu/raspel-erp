-- =====================================================================
-- RAZPEK RED TEAM - IZOLE TEST ORTAMI KURULUMU
-- Bu dosya YALNIZCA test ortami olusturmak icindir.
-- Tum kayitlar "ZZTEST_" oneki ile isaretlenir ve 99 (bilinmeyen/id) ile
-- kirletilmez. Temizlik: 99_RAZPEK_TEMIZLEME.sql
-- =====================================================================

BEGIN;

-- 1) Tenant B sirketi
INSERT INTO sistem.sirket (id, ad, vergi_no, aktif, olusturma_tarihi, negatif_stok_izni)
VALUES (99, 'ZZTEST Sirket B', '9999999999', true, now(), false);

-- 2) Kullanicilar
-- Parola: mevcut admin ile ayni BCrypt hash'i kopyalanir; kaynak koda parola
-- YAZILMAZ. Kurulumdan sonra gecici test parolasini su komutla belirleyin
-- (pgcrypto varsa):
--   UPDATE sistem.kullanici SET password = crypt('<parola>', gen_salt('bf'))
--   WHERE id >= 9900;
-- Alternatif: token_al.ps1 -Pass parametresi ile gecici parolayi verin.
-- id araligi 9901-9906 -> mevcut kayitlarla cakismaz

INSERT INTO sistem.kullanici
  (id, username, password, display_name, sirket_id, role, active, olusturma_tarihi, token_version, saha_kullanici)
SELECT 9901, 'zzadmin_b', password, 'ZZTEST Admin B', 99, 'ADMIN', true, now(), 0, false
FROM sistem.kullanici WHERE username = 'admin';

INSERT INTO sistem.kullanici
  (id, username, password, display_name, sirket_id, role, active, olusturma_tarihi, token_version, saha_kullanici)
SELECT 9902, 'zzuser_b', password, 'ZZTEST User B', 99, 'USER', true, now(), 0, false
FROM sistem.kullanici WHERE username = 'admin';

INSERT INTO sistem.kullanici
  (id, username, password, display_name, sirket_id, role, active, olusturma_tarihi, token_version, saha_kullanici)
SELECT 9903, 'zzdriver_b', password, 'ZZTEST Driver B', 99, 'DRIVER', true, now(), 0, false
FROM sistem.kullanici WHERE username = 'admin';

INSERT INTO sistem.kullanici
  (id, username, password, display_name, sirket_id, role, active, olusturma_tarihi, token_version, saha_kullanici)
SELECT 9904, 'zzsaha_b', password, 'ZZTEST Saha B', 99, 'SAHA', true, now(), 0, false
FROM sistem.kullanici WHERE username = 'admin';

-- 3) Tenant A'da da test kullanicilari (ayni roller) -> karsilastirma icin
INSERT INTO sistem.kullanici
  (id, username, password, display_name, sirket_id, role, active, olusturma_tarihi, token_version, saha_kullanici)
SELECT 9905, 'zzuser_a', password, 'ZZTEST User A', 4, 'USER', true, now(), 0, false
FROM sistem.kullanici WHERE username = 'admin';

INSERT INTO sistem.kullanici
  (id, username, password, display_name, sirket_id, role, active, olusturma_tarihi, token_version, saha_kullanici)
SELECT 9906, 'zzdriver_a', password, 'ZZTEST Driver A', 4, 'DRIVER', true, now(), 0, false
FROM sistem.kullanici WHERE username = 'admin';

-- 4) Uyelik (coklu sirket)
INSERT INTO sistem.kullanici_sirket (kullanici_id, sirket_id) VALUES
  (9901, 99), (9902, 99), (9903, 99), (9904, 99),
  (9905, 4),  (9906, 4);

-- 5) Donem (Tenant B icin - rapor/ekstre uclari icin gerekli)
INSERT INTO sistem.donem (sirket_id, ad, baslangic, bitis, aktif, olusturma_tarihi, kilitli, version)
SELECT 99, 'ZZTEST 2026 Donemi', DATE '2026-01-01', DATE '2026-12-31', true, now(), false, 0
WHERE NOT EXISTS (SELECT 1 FROM sistem.donem WHERE sirket_id = 99);

COMMIT;