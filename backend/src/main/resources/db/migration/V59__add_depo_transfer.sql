-- V59__add_depo_transfer.sql
-- Depolar arası stok transferi onay akışı.
CREATE TABLE IF NOT EXISTS sube.depo_transfer (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT NOT NULL,
    kaynak_depo_id BIGINT NOT NULL,
    hedef_depo_id BIGINT NOT NULL,
    stok_id BIGINT NOT NULL,
    miktar NUMERIC(19,2) NOT NULL,
    durum VARCHAR(20) NOT NULL DEFAULT 'BEKLIYOR',
    aciklama VARCHAR(500),
    olusturan_kullanici_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL,
    onay_tarihi TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_depo_transfer_sirket ON sube.depo_transfer(sirket_id);
