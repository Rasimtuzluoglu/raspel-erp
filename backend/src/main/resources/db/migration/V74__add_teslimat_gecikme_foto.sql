-- V74__add_teslimat_gecikme_foto.sql
-- Teslimat: beklenen teslim tarihi (gecikme takibi) ve teslimat fotoğrafı.
ALTER TABLE ticaret.teslimat ADD COLUMN IF NOT EXISTS beklenen_teslim_tarihi DATE;
ALTER TABLE ticaret.teslimat ADD COLUMN IF NOT EXISTS teslimat_foto TEXT;
