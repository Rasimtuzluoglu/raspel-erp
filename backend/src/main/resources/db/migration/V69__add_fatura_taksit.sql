-- V69__add_fatura_taksit.sql
-- Satis faturasinda taksit bilgisi (kurum + cekilen tutar).
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS taksit_kurum VARCHAR(255);
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS taksit_tutar NUMERIC(19, 2);
