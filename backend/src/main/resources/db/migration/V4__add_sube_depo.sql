-- =============================================================
-- V4: Sube (Branch) ve Depo (Warehouse) yapisi
-- =============================================================

CREATE SCHEMA IF NOT EXISTS sube;

CREATE TABLE sube.sube (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(200) NOT NULL,
    adres VARCHAR(500),
    telefon VARCHAR(20),
    yetkili VARCHAR(100),
    sirket_id BIGINT NOT NULL,
    aktif BOOLEAN NOT NULL DEFAULT true,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE sube.depo (
    id BIGSERIAL PRIMARY KEY,
    ad VARCHAR(200) NOT NULL,
    adres VARCHAR(500),
    yetkili VARCHAR(100),
    sube_id BIGINT NOT NULL REFERENCES sube.sube(id),
    sirket_id BIGINT NOT NULL,
    aktif BOOLEAN NOT NULL DEFAULT true,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE sube.depo_stok (
    id BIGSERIAL PRIMARY KEY,
    depo_id BIGINT NOT NULL REFERENCES sube.depo(id),
    stok_id BIGINT NOT NULL REFERENCES stok.stok(id),
    miktar NUMERIC(19,2) NOT NULL DEFAULT 0,
    olusturma_tarihi TIMESTAMP NOT NULL,
    UNIQUE(depo_id, stok_id)
);
