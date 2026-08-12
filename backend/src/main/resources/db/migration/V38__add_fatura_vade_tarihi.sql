-- V38__add_fatura_vade_tarihi.sql
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS vade_tarihi DATE;
