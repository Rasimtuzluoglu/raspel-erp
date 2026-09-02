-- V65__add_fatura_odeme_kasa.sql
-- Odeme yontemi ve kasa secimi (POS entegrasyonu icin).
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS odeme_yontemi VARCHAR(20);
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS kasa_id BIGINT;
