-- V40__add_fatura_depo.sql
-- Alış faturasına depo seçimi ekler; malın hangi depoya gireceğini tutar.
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS depo_id BIGINT;
