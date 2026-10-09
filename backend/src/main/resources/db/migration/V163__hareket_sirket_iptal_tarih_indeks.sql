-- =====================================================================
-- V163: Hareket listesi icin birlestirilmis indeks
--
-- DENETIM BULGUSU:
--   Hareket listesi (uygulamanin en cok kullanilan ekrani) su sekilde sorgulaniyor:
--       WHERE sirket_id = ? AND iptal = false ORDER BY hareket_tarihi DESC
--
--   Mevcut indeksler bu sorgunun hicbirini tek basina karsilamiyor:
--     - idx_hareket_sirket_tarih (sirket_id, hareket_tarihi DESC): sirket ve
--       siralama icin optimal, ama `iptal` filtresi indeks disinda kalir;
--       iptalli satirlar okunup elenir (paylanmis tablo buyudukce yavaslar).
--     - idx_hareket_iptal (sirket_id, iptal): filtre icin optimal, ama
--       ORDER BY icin kullanilamaz; sonra siralama ayrica yapilir.
--
-- COZUM:
--   (sirket_id, iptal, hareket_tarihi DESC) birlestirilmis indeksi. Ayni
--   sekilde sorgulayan her liste ekranindan (hareket gecmisi, cari ekstresi,
--   tahsilat gecmisi) fayda gorur.
--
-- Idempotent.
-- =====================================================================

CREATE INDEX IF NOT EXISTS idx_hareket_sirket_iptal_tarih
    ON cari.hareket (sirket_id, iptal, hareket_tarihi DESC);