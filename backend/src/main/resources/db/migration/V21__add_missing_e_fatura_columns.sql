-- =============================================================
-- V21: E-Fatura Tablosuna Eksik Kolonların Eklenmesi
-- =============================================================

ALTER TABLE fatura.e_fatura ADD COLUMN IF NOT EXISTS alici_vkn_tckn VARCHAR(20);
ALTER TABLE fatura.e_fatura ADD COLUMN IF NOT EXISTS alici_unvan VARCHAR(250);
ALTER TABLE fatura.e_fatura ADD COLUMN IF NOT EXISTS odenecek_tutar NUMERIC(19,2);
