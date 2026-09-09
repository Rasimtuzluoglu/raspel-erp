-- V79__add_siparis_zincir_link.sql
-- Sipariş -> Üretim -> Sevk (irsaliye) -> Teslimat zinciri için siparis_id bağlantıları.
ALTER TABLE stok.uretim_emri ADD COLUMN IF NOT EXISTS siparis_id BIGINT;
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS siparis_id BIGINT;
ALTER TABLE ticaret.teslimat ADD COLUMN IF NOT EXISTS siparis_id BIGINT;
