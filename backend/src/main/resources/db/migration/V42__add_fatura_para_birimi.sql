-- V42__add_fatura_para_birimi.sql
-- Fatura para birimi (TRY/USD/EUR/GBP/SAR/GAU).
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS para_birimi VARCHAR(10) DEFAULT 'TRY';
