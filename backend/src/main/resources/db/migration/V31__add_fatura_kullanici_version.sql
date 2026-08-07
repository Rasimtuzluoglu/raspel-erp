-- V31: Fatura'ya satis yapan kisi + optimistic locking
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS olusturan_kullanici_id BIGINT;
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS olusturan_kullanici_adi VARCHAR(100);
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
