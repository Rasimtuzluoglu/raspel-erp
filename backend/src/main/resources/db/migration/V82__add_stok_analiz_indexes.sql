-- V82__add_stok_analiz_indexes.sql
-- Ürün maliyet / alış-satış ortalaması / kârlılık analiz sorguları için indexler.
-- Mevcut tablolara kolon eklenmez; yalnızca sorgu performansı. Veri bozulmaz.

CREATE INDEX IF NOT EXISTS idx_fatura_kalem_stok
    ON fatura.fatura_kalem(stok_id)
    WHERE stok_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_iade_kalem_stok
    ON ticaret.iade_kalem(stok_id)
    WHERE stok_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_iade_tur_durum_tarih
    ON ticaret.iade(tur, durum, tarih);