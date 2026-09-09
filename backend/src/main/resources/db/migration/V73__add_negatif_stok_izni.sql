-- V73__add_negatif_stok_izni.sql
-- Şirket bazında negatif stok izni (satışta yetersiz stokta satışa izin verme).
ALTER TABLE sistem.sirket ADD COLUMN IF NOT EXISTS negatif_stok_izni BOOLEAN NOT NULL DEFAULT FALSE;
