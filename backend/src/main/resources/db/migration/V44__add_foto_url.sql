-- V44__add_foto_url.sql
-- Cari hesap ve ürün görseli (fotoğraf) alanları.
ALTER TABLE cari.cari_hesap ADD COLUMN IF NOT EXISTS foto_url VARCHAR(500);
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS foto_url VARCHAR(500);
