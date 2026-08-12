-- V35__add_fatura_teslim_eden.sql
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS teslim_eden VARCHAR(100);
