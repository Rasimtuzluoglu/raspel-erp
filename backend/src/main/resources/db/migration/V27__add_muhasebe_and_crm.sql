-- =============================================================
-- V27: Genel Muhasebe Modülü + CRM (Fırsat Takibi)
-- =============================================================

CREATE SCHEMA IF NOT EXISTS muhasebe;

-- 1. Hesap Planı (Chart of Accounts)
CREATE TABLE IF NOT EXISTS muhasebe.hesap_plani (
    id BIGSERIAL PRIMARY KEY,
    kod VARCHAR(20) NOT NULL,
    ad VARCHAR(200) NOT NULL,
    tip VARCHAR(20) NOT NULL,           -- AKTIF / PASIF / GELIR / GIDER
    grup VARCHAR(100),
    ust_id BIGINT REFERENCES muhasebe.hesap_plani(id) ON DELETE SET NULL,
    sirket_id BIGINT,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_hesap_plani_kod_sirket UNIQUE (kod, sirket_id)
);

-- 2. Muhasebe Fişi (Yevmiye Kaydı)
CREATE TABLE IF NOT EXISTS muhasebe.muhasebe_fisi (
    id BIGSERIAL PRIMARY KEY,
    fis_no VARCHAR(30) NOT NULL,
    tarih DATE NOT NULL,
    aciklama VARCHAR(500),
    durum VARCHAR(20) NOT NULL DEFAULT 'KAYITLI',  -- KAYITLI / ONAYLANDI / IPTAL
    sirket_id BIGINT,
    kullanici_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Muhasebe Fişi Kalemleri (Borç/Alacak)
CREATE TABLE IF NOT EXISTS muhasebe.muhasebe_fis_kalem (
    id BIGSERIAL PRIMARY KEY,
    fis_id BIGINT NOT NULL REFERENCES muhasebe.muhasebe_fisi(id) ON DELETE CASCADE,
    hesap_kodu VARCHAR(20) NOT NULL,
    hesap_adi VARCHAR(200),
    borc NUMERIC(19,2) NOT NULL DEFAULT 0,
    alacak NUMERIC(19,2) NOT NULL DEFAULT 0,
    aciklama VARCHAR(500)
);

-- 4. CRM Fırsatlar (Potansiyel Müşteri Takibi)
CREATE TABLE IF NOT EXISTS ticaret.cari_firsat (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(250) NOT NULL,
    cari_hesap_id BIGINT,
    durum VARCHAR(30) NOT NULL DEFAULT 'YENI',  -- YENI / TEMAS / TEKLIF / KAZANILDI / KAYBEDILDI
    kaynak VARCHAR(50),
    deger NUMERIC(19,2),
    tahmini_kapanis DATE,
    aciklama VARCHAR(1000),
    kullanici_id BIGINT,
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hesap_plani_sirket ON muhasebe.hesap_plani(sirket_id);
CREATE INDEX IF NOT EXISTS idx_muhasebe_fisi_sirket_tarih ON muhasebe.muhasebe_fisi(sirket_id, tarih);
CREATE INDEX IF NOT EXISTS idx_fis_kalem_fis ON muhasebe.muhasebe_fis_kalem(fis_id);
CREATE INDEX IF NOT EXISTS idx_cari_firsat_sirket ON ticaret.cari_firsat(sirket_id);
