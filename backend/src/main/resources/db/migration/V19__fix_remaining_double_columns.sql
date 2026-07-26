-- =============================================================
-- V19: Kalan DOUBLE PRECISION → NUMERIC(19,2) dönüşümü
-- V6'da eksik kalan kolonlar
-- =============================================================

-- Stok fiyat alanı (V1'de DOUBLE PRECISION olarak oluşturulmuş, V6'da atlanmış)
ALTER TABLE stok.stok ALTER COLUMN fiyat TYPE NUMERIC(19,2) USING fiyat::NUMERIC(19,2);

-- İrsaliye kalem miktar alanı (muhasebe schema)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'muhasebe' AND table_name = 'irsaliye_kalem') THEN
        ALTER TABLE muhasebe.irsaliye_kalem ALTER COLUMN miktar TYPE NUMERIC(19,2) USING miktar::NUMERIC(19,2);
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'fatura' AND table_name = 'irsaliye_kalem') THEN
        ALTER TABLE fatura.irsaliye_kalem ALTER COLUMN miktar TYPE NUMERIC(19,2) USING miktar::NUMERIC(19,2);
    END IF;
END $$;
