-- Fatura -> Sipariş bağı. Sipariş geri alınınca bağlı faturanın iptal edilebilmesi ve
-- yeniden faturalamanın (çift fatura) engellenmesi için.
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS siparis_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_fatura_siparis ON fatura.fatura (siparis_id);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_fatura_siparis') THEN
        ALTER TABLE fatura.fatura ADD CONSTRAINT fk_fatura_siparis
            FOREIGN KEY (siparis_id) REFERENCES siparis.siparis(id) NOT VALID;
    END IF;
END $$;
ALTER TABLE fatura.fatura VALIDATE CONSTRAINT fk_fatura_siparis;
