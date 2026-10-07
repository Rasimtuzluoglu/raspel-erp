-- =====================================================================
-- V159: Cari çalışma para birimi (Faz 2.9)
--
-- Cari kartına para birimi eklenir (TRY/USD/EUR/GBP). Bakiye halen TL bazında
-- tutulur; bu alan carinin çalışma/doküman para birimini tanımlar.
--
-- Idempotent.
-- =====================================================================

ALTER TABLE cari.cari_hesap
    ADD COLUMN IF NOT EXISTS para_birimi varchar(3) DEFAULT 'TRY';

UPDATE cari.cari_hesap SET para_birimi = 'TRY' WHERE para_birimi IS NULL;
