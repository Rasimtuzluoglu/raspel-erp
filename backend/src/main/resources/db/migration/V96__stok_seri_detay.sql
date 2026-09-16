-- V96__stok_seri_detay.sql
-- Seri/lot/SKT takibini operasyonel hale getirir: depo, miktar, kalan ve durum.
-- Mevcut kayitlar tek birimlik seri kabul edilir (miktar=kalan=1, STOKTA).
ALTER TABLE envanter.stok_seri ADD COLUMN IF NOT EXISTS depo_id BIGINT;
ALTER TABLE envanter.stok_seri ADD COLUMN IF NOT EXISTS miktar NUMERIC(19, 2) DEFAULT 1;
ALTER TABLE envanter.stok_seri ADD COLUMN IF NOT EXISTS kalan_miktar NUMERIC(19, 2) DEFAULT 1;
ALTER TABLE envanter.stok_seri ADD COLUMN IF NOT EXISTS durum VARCHAR(20) DEFAULT 'STOKTA';
ALTER TABLE envanter.stok_seri ADD COLUMN IF NOT EXISTS giris_tarihi DATE;

UPDATE envanter.stok_seri SET miktar = 1 WHERE miktar IS NULL;
UPDATE envanter.stok_seri SET kalan_miktar = 1 WHERE kalan_miktar IS NULL;
UPDATE envanter.stok_seri SET durum = 'STOKTA' WHERE durum IS NULL;
UPDATE envanter.stok_seri SET giris_tarihi = olusturma_tarihi::date WHERE giris_tarihi IS NULL;

CREATE INDEX IF NOT EXISTS idx_stok_seri_depo ON envanter.stok_seri(depo_id);
CREATE INDEX IF NOT EXISTS idx_stok_seri_durum ON envanter.stok_seri(durum);
