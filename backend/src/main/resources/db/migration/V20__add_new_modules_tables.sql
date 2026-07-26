-- =============================================================
-- V20: E-Fatura, Döviz Kuru ve Rol & Yetki Matrisi Tabloları
-- =============================================================

-- Schema'lar
CREATE SCHEMA IF NOT EXISTS fatura;
CREATE SCHEMA IF NOT EXISTS finans;
CREATE SCHEMA IF NOT EXISTS sistem;

-- 1. E-Fatura Tablosu
CREATE TABLE IF NOT EXISTS fatura.e_fatura (
    id BIGSERIAL PRIMARY KEY,
    fatura_id BIGINT NOT NULL,
    ettn VARCHAR(50) NOT NULL UNIQUE,
    fatura_no VARCHAR(50),
    senaryo VARCHAR(50) NOT NULL,
    tip VARCHAR(50) NOT NULL,
    gib_durum_kodu INTEGER DEFAULT 1000,
    gib_durum_aciklama VARCHAR(255),
    ubl_xml TEXT,
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    guncelleme_tarihi TIMESTAMP
);

-- 2. Döviz Kuru Tablosu
CREATE TABLE IF NOT EXISTS finans.doviz_kuru (
    id BIGSERIAL PRIMARY KEY,
    doviz_kodu VARCHAR(10) NOT NULL,
    doviz_adi VARCHAR(50) NOT NULL,
    tarih DATE NOT NULL,
    alis_kuru NUMERIC(19,4) NOT NULL,
    satis_kuru NUMERIC(19,4) NOT NULL,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_doviz_kuru_kod_tarih UNIQUE (doviz_kodu, tarih)
);

-- 3. Yetki Tablosu
CREATE TABLE IF NOT EXISTS sistem.yetki (
    id BIGSERIAL PRIMARY KEY,
    kod VARCHAR(50) NOT NULL UNIQUE,
    modul VARCHAR(50) NOT NULL,
    aciklama VARCHAR(150)
);

-- 4. Rol Tablosu
CREATE TABLE IF NOT EXISTS sistem.rol (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(50) NOT NULL UNIQUE,
    aciklama VARCHAR(150)
);

-- 5. Rol-Yetki Matrisi İlişki Tablosu
CREATE TABLE IF NOT EXISTS sistem.rol_yetki (
    rol_id BIGINT NOT NULL REFERENCES sistem.rol(id) ON DELETE CASCADE,
    yetki_id BIGINT NOT NULL REFERENCES sistem.yetki(id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, yetki_id)
);
