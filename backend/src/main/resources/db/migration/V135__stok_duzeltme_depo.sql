-- Stok düzeltmesi artık seçilen depoya işlenir; boşsa varsayılan depo kullanılır.
ALTER TABLE stok.stok_duzeltme ADD COLUMN IF NOT EXISTS depo_id BIGINT;
