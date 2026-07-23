-- =============================================================
-- V7: Optimistic locking icin version kolonlari
-- =============================================================

ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
ALTER TABLE muhasebe.kasa ADD COLUMN IF NOT EXISTS version INTEGER DEFAULT 0;
