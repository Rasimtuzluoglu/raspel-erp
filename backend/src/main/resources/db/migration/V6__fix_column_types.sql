-- =============================================================
-- V6: Double -> BigDecimal donusumu icin kolon tipleri
-- Tablolar V1'de float8 (double) olarak olusturuldu,
-- entity'lerde BigDecimal'a (numeric(19,2)) cevrildi
-- =============================================================

ALTER TABLE muhasebe.cek_senet ALTER COLUMN tutar TYPE NUMERIC(19,2) USING tutar::NUMERIC(19,2);
ALTER TABLE stok.stok ALTER COLUMN miktar TYPE NUMERIC(19,2) USING miktar::NUMERIC(19,2);
ALTER TABLE stok.stok ALTER COLUMN min_miktar TYPE NUMERIC(19,2) USING min_miktar::NUMERIC(19,2);

-- Siparis tablosu
ALTER TABLE siparis.siparis ALTER COLUMN ara_toplam TYPE NUMERIC(19,2) USING ara_toplam::NUMERIC(19,2);
ALTER TABLE siparis.siparis ALTER COLUMN kdv TYPE NUMERIC(19,2) USING kdv::NUMERIC(19,2);
ALTER TABLE siparis.siparis ALTER COLUMN genel_toplam TYPE NUMERIC(19,2) USING genel_toplam::NUMERIC(19,2);
ALTER TABLE siparis.siparis_kalem ALTER COLUMN miktar TYPE NUMERIC(19,2) USING miktar::NUMERIC(19,2);
ALTER TABLE siparis.siparis_kalem ALTER COLUMN birim_fiyat TYPE NUMERIC(19,2) USING birim_fiyat::NUMERIC(19,2);
ALTER TABLE siparis.siparis_kalem ALTER COLUMN kdv_orani TYPE NUMERIC(5,2) USING kdv_orani::NUMERIC(5,2);
ALTER TABLE siparis.siparis_kalem ALTER COLUMN tutar TYPE NUMERIC(19,2) USING tutar::NUMERIC(19,2);

-- Satinalma siparis
ALTER TABLE satinalma.satinalma_siparis ALTER COLUMN ara_toplam TYPE NUMERIC(19,2) USING ara_toplam::NUMERIC(19,2);
ALTER TABLE satinalma.satinalma_siparis ALTER COLUMN kdv TYPE NUMERIC(19,2) USING kdv::NUMERIC(19,2);
ALTER TABLE satinalma.satinalma_siparis ALTER COLUMN genel_toplam TYPE NUMERIC(19,2) USING genel_toplam::NUMERIC(19,2);
ALTER TABLE satinalma.satinalma_siparis_kalem ALTER COLUMN miktar TYPE NUMERIC(19,2) USING miktar::NUMERIC(19,2);
ALTER TABLE satinalma.satinalma_siparis_kalem ALTER COLUMN birim_fiyat TYPE NUMERIC(19,2) USING birim_fiyat::NUMERIC(19,2);
ALTER TABLE satinalma.satinalma_siparis_kalem ALTER COLUMN kdv_orani TYPE NUMERIC(5,2) USING kdv_orani::NUMERIC(5,2);
ALTER TABLE satinalma.satinalma_siparis_kalem ALTER COLUMN tutar TYPE NUMERIC(19,2) USING tutar::NUMERIC(19,2);

-- Satinalma talep
ALTER TABLE satinalma.satinalma_talep_kalem ALTER COLUMN miktar TYPE NUMERIC(19,2) USING miktar::NUMERIC(19,2);
ALTER TABLE satinalma.satinalma_talep_kalem ALTER COLUMN tahmini_birim_fiyat TYPE NUMERIC(19,2) USING tahmini_birim_fiyat::NUMERIC(19,2);

-- Personel
ALTER TABLE personel.personel ALTER COLUMN maas TYPE NUMERIC(19,2) USING maas::NUMERIC(19,2);

-- Stok hareket
ALTER TABLE stok.stok_hareket ALTER COLUMN miktar TYPE NUMERIC(19,2) USING miktar::NUMERIC(19,2);

-- Version (optimistic locking) kolonlari
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS version INTEGER;
ALTER TABLE muhasebe.kasa ADD COLUMN IF NOT EXISTS version INTEGER;
