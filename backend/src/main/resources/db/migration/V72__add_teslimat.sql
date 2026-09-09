-- V72__add_teslimat.sql
-- Satış/fatura modülüne entegre teslimat takibi.
CREATE TABLE IF NOT EXISTS ticaret.teslimat (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT,
    fatura_id BIGINT,
    fatura_numarasi VARCHAR(100),
    driver_id BIGINT,
    teslimat_adresi TEXT,
    musteri_adi VARCHAR(255),
    durum VARCHAR(20) NOT NULL DEFAULT 'BEKLEMEDE',
    notlar TEXT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now(),
    teslim_tarihi TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_teslimat_sirket ON ticaret.teslimat (sirket_id);
CREATE INDEX IF NOT EXISTS idx_teslimat_driver ON ticaret.teslimat (driver_id);
CREATE INDEX IF NOT EXISTS idx_teslimat_sirket_durum ON ticaret.teslimat (sirket_id, durum);
