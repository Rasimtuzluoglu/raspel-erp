-- V83__add_taksit.sql
-- Taksit plani: satis/tahsilat icin vade bazli taksit kalemleri.
CREATE SCHEMA IF NOT EXISTS finans;

CREATE TABLE IF NOT EXISTS finans.taksit (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT NOT NULL,
    cari_id BIGINT NOT NULL,
    fatura_id BIGINT,
    hareket_id BIGINT,
    plan_no VARCHAR(40) NOT NULL,
    kurum VARCHAR(255),
    taksit_no INTEGER NOT NULL,
    taksit_sayisi INTEGER NOT NULL,
    vade_tarihi DATE NOT NULL,
    tutar NUMERIC(19, 2) NOT NULL,
    odeme_durumu VARCHAR(20) NOT NULL DEFAULT 'BEKLEMEDE',
    odeme_tarihi DATE,
    aciklama VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_taksit_sirket_vade ON finans.taksit (sirket_id, vade_tarihi, odeme_durumu);
CREATE INDEX IF NOT EXISTS idx_taksit_sirket_cari ON finans.taksit (sirket_id, cari_id);
CREATE INDEX IF NOT EXISTS idx_taksit_plan_no ON finans.taksit (plan_no);
