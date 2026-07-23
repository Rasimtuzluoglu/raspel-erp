-- =============================================================
-- V5: Tenant izolasyonu icin sirket_id kolonlari
-- Tablolar V1'de hangi schema'da olusturulduysa ona gore
-- =============================================================

ALTER TABLE cari.cari_hesap ADD COLUMN IF NOT EXISTS sirket_id BIGINT;
ALTER TABLE muhasebe.banka ADD COLUMN IF NOT EXISTS sirket_id BIGINT;
ALTER TABLE muhasebe.kasa ADD COLUMN IF NOT EXISTS sirket_id BIGINT;
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS sirket_id BIGINT;
ALTER TABLE cari.hareket ADD COLUMN IF NOT EXISTS sirket_id BIGINT;
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS sirket_id BIGINT;
ALTER TABLE sistem.gelir_gider_kategori ADD COLUMN IF NOT EXISTS sirket_id BIGINT;
