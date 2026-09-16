-- V95__irsaliye_depo.sql
-- Sevk/irsaliye icin depo secimi (stok cikis/giris deposu) ve depo bazli stok senkronu.
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS depo_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_irsaliye_depo ON muhasebe.irsaliye(depo_id);
