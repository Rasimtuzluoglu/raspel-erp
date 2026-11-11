-- V53 zaten fatura_id uzerinde tekil indeks (uk_efatura_fatura) olusturuyor.
-- V179 ile eklenen ikinci tekil indeks gereksiz; yazma maliyetini azaltmak icin kaldir.
DROP INDEX IF EXISTS fatura.uk_efatura_fatura_id;
