-- Ürün kartına varsayılan depo alanı. Stok giriş/çıkışında ön seçili gelir.
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS varsayilan_depo_id BIGINT;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_stok_varsayilan_depo') THEN
        ALTER TABLE stok.stok ADD CONSTRAINT fk_stok_varsayilan_depo
            FOREIGN KEY (varsayilan_depo_id) REFERENCES sube.depo(id) NOT VALID;
    END IF;
END $$;
ALTER TABLE stok.stok VALIDATE CONSTRAINT fk_stok_varsayilan_depo;
