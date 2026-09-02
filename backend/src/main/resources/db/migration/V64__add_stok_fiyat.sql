-- V64__add_stok_fiyat.sql
-- Bir urun icin birden fazla fiyat tanimi (Perakende, Toptan, Kurumsal, vb.)
CREATE TABLE IF NOT EXISTS stok.stok_fiyat (
    id BIGSERIAL PRIMARY KEY,
    stok_id BIGINT NOT NULL REFERENCES stok.stok(id) ON DELETE CASCADE,
    ad VARCHAR(100) NOT NULL,
    fiyat NUMERIC(19,2) NOT NULL DEFAULT 0,
    sirket_id BIGINT NOT NULL,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_stok_fiyat_stok ON stok.stok_fiyat(stok_id);
