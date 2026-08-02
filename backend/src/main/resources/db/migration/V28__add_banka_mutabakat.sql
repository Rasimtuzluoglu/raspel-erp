-- =============================================================
-- V28: Banka Mutabakatı (Banka Hesap Özeti Eşleştirme)
-- =============================================================

CREATE TABLE IF NOT EXISTS finans.banka_hareketi (
    id BIGSERIAL PRIMARY KEY,
    banka_id BIGINT,
    tarih DATE NOT NULL,
    aciklama VARCHAR(500),
    borc NUMERIC(19,2) NOT NULL DEFAULT 0,
    alacak NUMERIC(19,2) NOT NULL DEFAULT 0,
    bakiye NUMERIC(19,2),
    eslesen_fatura_id BIGINT,
    eslestirildi BOOLEAN NOT NULL DEFAULT FALSE,
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_banka_hareketi_banka ON finans.banka_hareketi(banka_id);
CREATE INDEX IF NOT EXISTS idx_banka_hareketi_sirket ON finans.banka_hareketi(sirket_id);
CREATE INDEX IF NOT EXISTS idx_banka_hareketi_eslesme ON finans.banka_hareketi(eslestirildi);
