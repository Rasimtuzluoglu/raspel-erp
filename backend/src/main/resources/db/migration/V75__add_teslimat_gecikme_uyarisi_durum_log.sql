-- V75__add_teslimat_gecikme_uyarisi_durum_log.sql
-- Teslimat: gecikme bildirimi bayragi ve durum gecisi tarihcesi.
ALTER TABLE ticaret.teslimat ADD COLUMN IF NOT EXISTS gecikme_bildirildi BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS ticaret.teslimat_durum_log (
    id BIGSERIAL PRIMARY KEY,
    teslimat_id BIGINT NOT NULL,
    onceki_durum VARCHAR(20),
    yeni_durum VARCHAR(20),
    kullanici_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_teslimat_durum_log_teslimat ON ticaret.teslimat_durum_log (teslimat_id);
