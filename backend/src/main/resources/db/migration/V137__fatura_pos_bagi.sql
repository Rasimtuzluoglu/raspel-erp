-- Hızlı satışta (perakende POS) kart tahsilatının hangi POS terminalinden geçtiği
-- ve komisyon/valör bilgisi faturada saklanır; POS gün sonu bu satışları da kapsar.
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS pos_terminali_id BIGINT;
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS pos_ad VARCHAR(150);
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS komisyon_tutar NUMERIC(19,2);
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS valor_tarihi DATE;
CREATE INDEX IF NOT EXISTS idx_fatura_pos ON fatura.fatura (pos_terminali_id);
