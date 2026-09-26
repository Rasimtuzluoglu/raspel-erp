-- Çek/Senet -> Fatura bağı. Tahsil edilen çek/senet bir faturaya bağlanabilir; böylece
-- cari hareketi ve faturanın kalan/ödeme durumu tutarlı güncellenir.
ALTER TABLE muhasebe.cek_senet ADD COLUMN IF NOT EXISTS fatura_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_cek_senet_fatura ON muhasebe.cek_senet (fatura_id);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cek_senet_fatura') THEN
        ALTER TABLE muhasebe.cek_senet ADD CONSTRAINT fk_cek_senet_fatura
            FOREIGN KEY (fatura_id) REFERENCES fatura.fatura(id) NOT VALID;
    END IF;
END $$;
ALTER TABLE muhasebe.cek_senet VALIDATE CONSTRAINT fk_cek_senet_fatura;
