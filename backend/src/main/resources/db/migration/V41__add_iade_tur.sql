-- V41__add_iade_tur.sql
-- İade türü (SATIS iadesi = müşteriden dönen mal, ALIS iadesi = tedarikçiye iade).
ALTER TABLE ticaret.iade ADD COLUMN IF NOT EXISTS tur VARCHAR(20) DEFAULT 'SATIS';
