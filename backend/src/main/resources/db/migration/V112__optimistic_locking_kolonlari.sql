-- V112__optimistic_locking_kolonlari.sql
-- Es zamanli isteklerde kayip guncellemeyi (lost update) onlemek icin surum kolonlari.
-- fatura.fatura (V31), stok.stok ve muhasebe.kasa zaten version kolonuna sahiptir.

ALTER TABLE cari.cari_hesap ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE muhasebe.banka ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
