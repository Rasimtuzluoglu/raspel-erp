-- V66__add_cari_fiyat.sql
-- Cariye ozel fiyat listesi (bu musteri bu urunu su fiyattan alir).
CREATE TABLE IF NOT EXISTS cari.cari_fiyat (
    id BIGSERIAL PRIMARY KEY,
    cari_hesap_id BIGINT NOT NULL REFERENCES cari.cari_hesap(id) ON DELETE CASCADE,
    stok_id BIGINT NOT NULL REFERENCES stok.stok(id) ON DELETE CASCADE,
    fiyat NUMERIC(19,2) NOT NULL DEFAULT 0,
    sirket_id BIGINT NOT NULL,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (cari_hesap_id, stok_id)
);
CREATE INDEX IF NOT EXISTS idx_cari_fiyat_cari ON cari.cari_fiyat(cari_hesap_id);
