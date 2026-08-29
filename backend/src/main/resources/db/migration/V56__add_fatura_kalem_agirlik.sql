-- V56__add_fatura_kalem_agirlik.sql
-- Fatura kalemlerine birim ağırlık (kg) ekler; toplam yükleme ağırlığı hesabı için kullanılır.
ALTER TABLE fatura.fatura_kalem ADD COLUMN IF NOT EXISTS agirlik NUMERIC(10,2);
