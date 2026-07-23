-- V9: Stok detay alanlari (KDV, grup, barkod, raf)
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS kdv_orani NUMERIC(5,2);
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS stok_grubu VARCHAR(100);
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS barkod VARCHAR(100);
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS raf_no VARCHAR(50);
