-- Allow null cari_hesap_id in fatura table for anlik musteri sales
ALTER TABLE fatura.fatura ALTER COLUMN cari_hesap_id DROP NOT NULL;

-- Add default to olusturma_tarihi in fatura_kalem
ALTER TABLE fatura.fatura_kalem ALTER COLUMN olusturma_tarihi SET DEFAULT CURRENT_TIMESTAMP;
