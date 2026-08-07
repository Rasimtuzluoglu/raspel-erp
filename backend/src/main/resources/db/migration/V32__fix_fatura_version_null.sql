-- V32: Mevcut fatura kayitlarinda version NULL olanlari 0 yap
UPDATE fatura.fatura SET version = 0 WHERE version IS NULL;
