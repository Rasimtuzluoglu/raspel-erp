-- V84__add_cari_temsilci.sql
-- Cari hesaba satis temsilcisi bilgisi eklenir (Musteri 360 karti icin).
ALTER TABLE cari.cari_hesap ADD COLUMN IF NOT EXISTS temsilci_id BIGINT;
ALTER TABLE cari.cari_hesap ADD COLUMN IF NOT EXISTS temsilci_ad VARCHAR(255);
