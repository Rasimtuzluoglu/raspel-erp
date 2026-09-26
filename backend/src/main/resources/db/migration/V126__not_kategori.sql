-- Not kategorisi (ör. SAHA_ZIYARET, SAHA_NOT). Serbest metin; mevcut notlar NULL kalır.
ALTER TABLE sistem.notlar ADD COLUMN IF NOT EXISTS kategori VARCHAR(40);
CREATE INDEX IF NOT EXISTS idx_notlar_kategori ON sistem.notlar (kategori);
