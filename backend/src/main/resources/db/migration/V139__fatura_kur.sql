-- Dövizli faturalarda kayıt anındaki kur saklanır; dönem sonu kur değerlemesi
-- (kambiyo kâr/zararı) bu kur ile güncel TCMB kuru karşılaştırılarak hesaplanır.
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS kur NUMERIC(19,6);
