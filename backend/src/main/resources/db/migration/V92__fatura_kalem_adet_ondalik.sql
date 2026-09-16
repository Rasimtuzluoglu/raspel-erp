-- V92__fatura_kalem_adet_ondalik.sql
-- Sorun: fatura kalem adedi INTEGER idi; kg/metre gibi ondalikli birimlerde
-- (SiparisService/TeklifService intValue()) miktar kirpilip veri kaybina yol aciliyordu.
-- Cozum: adet kolonlarini NUMERIC(19,2) yap.
ALTER TABLE fatura.fatura_kalem
    ALTER COLUMN adet TYPE NUMERIC(19, 2) USING adet::numeric;

ALTER TABLE fatura.tekrarlayan_fatura_kalem
    ALTER COLUMN adet TYPE NUMERIC(19, 2) USING adet::numeric;
