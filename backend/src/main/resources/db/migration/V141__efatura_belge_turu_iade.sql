-- E-belge altyapısı: belge türü (EFATURA/EARSIV/EIRSALIYE/EIADE) ve iade bağı.
-- Kredi notu (iade) belgeleri iade kaydına bağlanır; belge türü listede/filtrede kullanılır.
ALTER TABLE fatura.e_fatura ADD COLUMN IF NOT EXISTS belge_turu VARCHAR(20);
ALTER TABLE fatura.e_fatura ADD COLUMN IF NOT EXISTS iade_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_efatura_iade ON fatura.e_fatura (iade_id);
