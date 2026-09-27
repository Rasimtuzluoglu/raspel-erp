-- Faturasız iadelerde cari bağı ve iade para iadesi (kasa/banka) alanları.
-- Önceden faturasız iade cariye hiç yansımıyordu ve para iadesi kaydı yoktu.
ALTER TABLE ticaret.iade ADD COLUMN IF NOT EXISTS cari_hesap_id BIGINT;
ALTER TABLE ticaret.iade ADD COLUMN IF NOT EXISTS kasa_id BIGINT;
ALTER TABLE ticaret.iade ADD COLUMN IF NOT EXISTS banka_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_iade_cari ON ticaret.iade (cari_hesap_id);
