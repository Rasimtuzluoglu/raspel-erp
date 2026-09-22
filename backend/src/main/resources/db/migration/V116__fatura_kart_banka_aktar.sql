-- Fatura: KART tahsilatının doğrudan banka hesabına aktarılabilmesi için banka alanları.
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS banka_id BIGINT;
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS karta_banka_aktar BOOLEAN;

CREATE INDEX IF NOT EXISTS idx_fatura_banka ON fatura.fatura (banka_id);
