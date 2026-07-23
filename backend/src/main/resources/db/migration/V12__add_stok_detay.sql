-- V12: Stok detay alanlari (marka, satis fiyati, agirlik, kategori)
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS marka VARCHAR(100);
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS satis_fiyati NUMERIC(19,2);
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS agirlik NUMERIC(10,2);
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS kategori VARCHAR(100);
