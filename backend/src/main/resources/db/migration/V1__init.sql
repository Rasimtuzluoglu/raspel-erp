-- =============================================================
-- RasPel ERP - V1: Initial Schema & Table Creation (consolidated)
-- V6/V7/V13/V14/V15/V18/V19 absorbed into this migration
-- =============================================================

-- 1. CREATE SCHEMAS
-- =============================================================

CREATE SCHEMA IF NOT EXISTS sistem;
CREATE SCHEMA IF NOT EXISTS cari;
CREATE SCHEMA IF NOT EXISTS stok;
CREATE SCHEMA IF NOT EXISTS fatura;
CREATE SCHEMA IF NOT EXISTS siparis;
CREATE SCHEMA IF NOT EXISTS satinalma;
CREATE SCHEMA IF NOT EXISTS muhasebe;
CREATE SCHEMA IF NOT EXISTS personel;
CREATE SCHEMA IF NOT EXISTS proje;

-- 2. SISTEM SCHEMA (shared/system tables)
-- =============================================================

CREATE TABLE sistem.sirket (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(200) NOT NULL,
    vergi_no VARCHAR(50),
    vergi_dairesi VARCHAR(100),
    adres VARCHAR(500),
    telefon VARCHAR(20),
    email VARCHAR(100),
    web_site VARCHAR(200),
    aktif BOOLEAN NOT NULL DEFAULT true,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE sistem.donem (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT NOT NULL,
    ad VARCHAR(100) NOT NULL,
    baslangic DATE NOT NULL,
    bitis DATE NOT NULL,
    aktif BOOLEAN NOT NULL DEFAULT true,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE sistem.kullanici (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    company_name VARCHAR(200),
    sirket_id BIGINT,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    active BOOLEAN NOT NULL DEFAULT true,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE sistem.gelir_gider_kategori (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(200) NOT NULL,
    tur VARCHAR(10) NOT NULL,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE sistem.audit_log (
    id BIGSERIAL PRIMARY KEY,
    kullanici_id BIGINT,
    islem VARCHAR(50) NOT NULL,
    entity_adi VARCHAR(100),
    entity_id BIGINT,
    aciklama TEXT,
    ip_adresi VARCHAR(50),
    tarih TIMESTAMP NOT NULL
);

-- 3. CARI SCHEMA
-- =============================================================

CREATE TABLE cari.cari_hesap (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(255) NOT NULL,
    vergi_numarasi VARCHAR(50),
    telefon VARCHAR(20),
    bakiye NUMERIC(19,2) NOT NULL DEFAULT 0,
    olusturma_tarihi TIMESTAMP NOT NULL,
    guncelleme_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE cari.hareket (
    id BIGSERIAL PRIMARY KEY,
    cari_hesap_id BIGINT NOT NULL REFERENCES cari.cari_hesap(id),
    tur VARCHAR(20) NOT NULL,
    tutar NUMERIC(19,2) NOT NULL,
    hareket_tarihi DATE NOT NULL,
    aciklama VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL
);

-- 4. STOK SCHEMA (all NUMERIC types - fixed from V6/V19)
-- =============================================================

CREATE TABLE stok.stok (
    id BIGSERIAL PRIMARY KEY,
    stok_kodu VARCHAR(50),
    ad VARCHAR(300) NOT NULL,
    birim VARCHAR(50),
    fiyat NUMERIC(19,2) NOT NULL DEFAULT 0,
    miktar NUMERIC(19,2) NOT NULL DEFAULT 0,
    min_miktar NUMERIC(19,2),
    aciklama VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL,
    version INTEGER DEFAULT 0
);

CREATE TABLE stok.stok_hareket (
    id BIGSERIAL PRIMARY KEY,
    stok_id BIGINT NOT NULL REFERENCES stok.stok(id),
    tur VARCHAR(20) NOT NULL,
    miktar NUMERIC(19,2) NOT NULL,
    hareket_tarihi DATE NOT NULL,
    aciklama VARCHAR(500),
    cari_hesap_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL
);

-- 5. FATURA SCHEMA (cari_hesap_id nullable for walk-in customers - from V13)
-- =============================================================

CREATE TABLE fatura.fatura (
    id BIGSERIAL PRIMARY KEY,
    fatura_numarasi VARCHAR(255) NOT NULL UNIQUE,
    tarih DATE NOT NULL,
    tur VARCHAR(20) NOT NULL,
    durum VARCHAR(20) NOT NULL DEFAULT 'TASLAK',
    cari_hesap_id BIGINT,
    aciklama VARCHAR(500),
    ara_toplam NUMERIC(19,2) NOT NULL,
    kdv NUMERIC(19,2) NOT NULL,
    genel_toplam NUMERIC(19,2) NOT NULL,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE fatura.fatura_kalem (
    id BIGSERIAL PRIMARY KEY,
    fatura_id BIGINT NOT NULL REFERENCES fatura.fatura(id),
    aciklama VARCHAR(300) NOT NULL,
    adet INTEGER NOT NULL,
    birim_fiyat NUMERIC(19,2) NOT NULL,
    kdv_orani NUMERIC(19,2) NOT NULL,
    tutar NUMERIC(19,2) NOT NULL,
    stok_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. SIPARIS SCHEMA (cari_hesap_id nullable, all NUMERIC - from V15/V6)
-- =============================================================

CREATE TABLE siparis.siparis (
    id BIGSERIAL PRIMARY KEY,
    siparis_no VARCHAR(50) NOT NULL UNIQUE,
    tarih DATE NOT NULL,
    cari_hesap_id BIGINT,
    tur VARCHAR(20) DEFAULT 'SATIS',
    durum VARCHAR(20) NOT NULL DEFAULT 'TEKLIF',
    ara_toplam NUMERIC(19,2),
    kdv NUMERIC(19,2),
    genel_toplam NUMERIC(19,2),
    aciklama VARCHAR(500),
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE siparis.siparis_kalem (
    id BIGSERIAL PRIMARY KEY,
    siparis_id BIGINT NOT NULL REFERENCES siparis.siparis(id),
    stok_id BIGINT,
    aciklama VARCHAR(200),
    miktar NUMERIC(19,2) NOT NULL,
    birim VARCHAR(20),
    birim_fiyat NUMERIC(19,2),
    kdv_orani NUMERIC(5,2),
    tutar NUMERIC(19,2),
    olusturma_tarihi TIMESTAMP NOT NULL
);

-- 7. SATINALMA SCHEMA (all NUMERIC - from V6)
-- =============================================================

CREATE TABLE satinalma.satinalma_talep (
    id BIGSERIAL PRIMARY KEY,
    talep_no VARCHAR(50) NOT NULL UNIQUE,
    tarih DATE NOT NULL,
    talep_eden VARCHAR(100),
    departman VARCHAR(100),
    durum VARCHAR(20) NOT NULL DEFAULT 'TASLAK',
    aciklama VARCHAR(500),
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE satinalma.satinalma_talep_kalem (
    id BIGSERIAL PRIMARY KEY,
    talep_id BIGINT NOT NULL REFERENCES satinalma.satinalma_talep(id),
    stok_id BIGINT,
    aciklama VARCHAR(200),
    miktar NUMERIC(19,2) NOT NULL,
    birim VARCHAR(20),
    tahmini_birim_fiyat NUMERIC(19,2),
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE satinalma.satinalma_siparis (
    id BIGSERIAL PRIMARY KEY,
    siparis_no VARCHAR(50) NOT NULL UNIQUE,
    tarih DATE NOT NULL,
    cari_hesap_id BIGINT NOT NULL,
    talep_id BIGINT,
    durum VARCHAR(20) NOT NULL DEFAULT 'TASLAK',
    ara_toplam NUMERIC(19,2),
    kdv NUMERIC(19,2),
    genel_toplam NUMERIC(19,2),
    aciklama VARCHAR(500),
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE satinalma.satinalma_siparis_kalem (
    id BIGSERIAL PRIMARY KEY,
    siparis_id BIGINT NOT NULL REFERENCES satinalma.satinalma_siparis(id),
    stok_id BIGINT,
    aciklama VARCHAR(200),
    miktar NUMERIC(19,2) NOT NULL,
    birim VARCHAR(20),
    birim_fiyat NUMERIC(19,2),
    kdv_orani NUMERIC(5,2),
    tutar NUMERIC(19,2),
    olusturma_tarihi TIMESTAMP NOT NULL
);

-- 8. MUHASEBE SCHEMA (all NUMERIC, cari_hesap_id nullable - from V6/V15)
-- =============================================================

CREATE TABLE muhasebe.kasa (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(200) NOT NULL,
    bakiye NUMERIC(19,2) NOT NULL DEFAULT 0,
    olusturma_tarihi TIMESTAMP NOT NULL,
    version INTEGER DEFAULT 0
);

CREATE TABLE muhasebe.kasa_hareket (
    id BIGSERIAL PRIMARY KEY,
    kasa_id BIGINT NOT NULL REFERENCES muhasebe.kasa(id),
    tur VARCHAR(20) NOT NULL,
    tutar NUMERIC(19,2) NOT NULL,
    hareket_tarihi DATE NOT NULL,
    aciklama VARCHAR(500),
    kategori_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE muhasebe.banka (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(200) NOT NULL,
    hesap_no VARCHAR(50),
    iban VARCHAR(50),
    bakiye NUMERIC(19,2) NOT NULL DEFAULT 0,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE muhasebe.cek_senet (
    id BIGSERIAL PRIMARY KEY,
    tur VARCHAR(10) NOT NULL,
    cari_hesap_id BIGINT,
    banka_adi VARCHAR(100),
    sube VARCHAR(100),
    cek_no VARCHAR(50),
    hesap_no VARCHAR(50),
    vade_tarihi DATE NOT NULL,
    kesinme_tarihi DATE,
    tutar NUMERIC(19,2) NOT NULL,
    durum VARCHAR(20) NOT NULL DEFAULT 'PORTFOY',
    aciklama VARCHAR(500),
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE muhasebe.irsaliye (
    id BIGSERIAL PRIMARY KEY,
    irsaliye_no VARCHAR(50) NOT NULL UNIQUE,
    tarih DATE NOT NULL,
    cari_hesap_id BIGINT,
    fatura_id BIGINT,
    durum VARCHAR(20) NOT NULL DEFAULT 'TASLAK',
    tur VARCHAR(10) NOT NULL DEFAULT 'SATIS',
    aciklama VARCHAR(500),
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE muhasebe.irsaliye_kalem (
    id BIGSERIAL PRIMARY KEY,
    irsaliye_id BIGINT NOT NULL REFERENCES muhasebe.irsaliye(id),
    stok_id BIGINT,
    aciklama VARCHAR(200),
    miktar NUMERIC(19,2) NOT NULL,
    birim VARCHAR(20),
    olusturma_tarihi TIMESTAMP NOT NULL
);

-- 9. PERSONEL SCHEMA (maas NUMERIC - from V6)
-- =============================================================

CREATE TABLE personel.personel (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(50) NOT NULL,
    soyad VARCHAR(50) NOT NULL,
    tc_kimlik VARCHAR(11) UNIQUE,
    dogum_tarihi DATE,
    ise_giris_tarihi DATE,
    cikis_tarihi DATE,
    departman VARCHAR(100),
    pozisyon VARCHAR(100),
    maas NUMERIC(19,2),
    telefon VARCHAR(20),
    email VARCHAR(100),
    adres VARCHAR(500),
    aktif BOOLEAN NOT NULL DEFAULT true,
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE personel.personel_izin (
    id BIGSERIAL PRIMARY KEY,
    personel_id BIGINT NOT NULL REFERENCES personel.personel(id),
    izin_turu VARCHAR(50) NOT NULL,
    baslangic DATE NOT NULL,
    bitis DATE NOT NULL,
    gun_sayisi INTEGER NOT NULL,
    durum VARCHAR(20) NOT NULL DEFAULT 'BEKLEMEDE',
    aciklama VARCHAR(500),
    onaylayan VARCHAR(100),
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE personel.personel_puantaj (
    id BIGSERIAL PRIMARY KEY,
    personel_id BIGINT NOT NULL REFERENCES personel.personel(id),
    tarih DATE NOT NULL,
    durum VARCHAR(20) NOT NULL,
    aciklama VARCHAR(200),
    olusturma_tarihi TIMESTAMP NOT NULL
);

-- 10. PROJE SCHEMA
-- =============================================================

CREATE TABLE proje.proje (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(200) NOT NULL,
    aciklama VARCHAR(1000),
    baslangic DATE NOT NULL,
    bitis DATE,
    durum VARCHAR(20) NOT NULL DEFAULT 'DEVAM_EDIYOR',
    sorumlu VARCHAR(100),
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE proje.gorev (
    id BIGSERIAL PRIMARY KEY,
    proje_id BIGINT NOT NULL REFERENCES proje.proje(id),
    ad VARCHAR(200) NOT NULL,
    aciklama VARCHAR(1000),
    durum VARCHAR(20) NOT NULL DEFAULT 'YAPILACAK',
    atanan VARCHAR(100),
    baslangic DATE,
    bitis DATE,
    olusturma_tarihi TIMESTAMP NOT NULL
);
