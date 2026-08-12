-- V36__add_fatura_teslimat_detay.sql
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS teslim_durumu VARCHAR(20);
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS teslim_notu TEXT;
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS teslim_fotograf TEXT;
