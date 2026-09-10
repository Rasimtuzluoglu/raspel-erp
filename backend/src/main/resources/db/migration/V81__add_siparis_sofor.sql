-- V81__add_siparis_sofor.sql
-- Siparişe şoför atama (teslimat takibi ile entegrasyon).
ALTER TABLE siparis.siparis ADD COLUMN IF NOT EXISTS driver_id BIGINT;
ALTER TABLE siparis.siparis ADD COLUMN IF NOT EXISTS driver_ad VARCHAR(100);
