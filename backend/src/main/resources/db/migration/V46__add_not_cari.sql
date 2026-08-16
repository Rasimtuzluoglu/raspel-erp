-- V46__add_not_cari.sql
-- Notlara cari hesap bağlantısı (cari görüşme notları).
ALTER TABLE sistem.notlar ADD COLUMN IF NOT EXISTS cari_hesap_id BIGINT;
