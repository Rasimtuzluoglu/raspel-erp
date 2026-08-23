ALTER TABLE sistem.sirket ADD COLUMN IF NOT EXISTS parent_id BIGINT REFERENCES sistem.sirket(id);
ALTER TABLE sistem.sirket ADD COLUMN IF NOT EXISTS tur VARCHAR(20) DEFAULT 'DIGER';
ALTER TABLE sistem.sirket ADD COLUMN IF NOT EXISTS yil INTEGER;
CREATE INDEX IF NOT EXISTS idx_sirket_parent ON sistem.sirket(parent_id);
CREATE INDEX IF NOT EXISTS idx_sirket_yil ON sistem.sirket(yil);
