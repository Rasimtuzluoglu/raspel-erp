-- V183: Depolar operasyon merkezi (Faz 11).
-- Yalnizca nullable kolon + indeks; mevcut veri korunur.

ALTER TABLE sube.depo ADD COLUMN IF NOT EXISTS kapasite_kalem INT;
ALTER TABLE sube.depo ADD COLUMN IF NOT EXISTS sorumlu_kullanici_id BIGINT;

ALTER TABLE sube.depo_transfer ADD COLUMN IF NOT EXISTS onaylayan_kullanici_id BIGINT;
ALTER TABLE sube.depo_transfer ADD COLUMN IF NOT EXISTS red_tarihi TIMESTAMPTZ;
ALTER TABLE sube.depo_transfer ADD COLUMN IF NOT EXISTS red_sebebi VARCHAR(500);

ALTER TABLE stok.stok_hareket ADD COLUMN IF NOT EXISTS kullanici_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_depo_transfer_durum
    ON sube.depo_transfer (sirket_id, durum, olusturma_tarihi);
CREATE INDEX IF NOT EXISTS idx_stok_hareket_depo
    ON stok.stok_hareket (depo_id, olusturma_tarihi);
