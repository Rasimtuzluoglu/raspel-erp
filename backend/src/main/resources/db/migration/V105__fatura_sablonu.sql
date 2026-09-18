-- V105__fatura_sablonu.sql
-- Fatura tasarim sablonu ve POS fis ayarlari sunucuda (sirket bazli) saklanir.
-- JSON olarak tutulur; PDF uretimi ve fis yazdirma bu ayarlari uygular.
ALTER TABLE sistem.sirket ADD COLUMN IF NOT EXISTS fatura_sablonu TEXT;
ALTER TABLE sistem.sirket ADD COLUMN IF NOT EXISTS pos_fis_ayarlari TEXT;
