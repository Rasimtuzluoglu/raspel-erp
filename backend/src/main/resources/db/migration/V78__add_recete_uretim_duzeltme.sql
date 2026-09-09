-- V78__add_recete_uretim_duzeltme.sql
-- Üretim: reçete (ürün ağacı), üretim emri ve stok düzeltme geçmişi.
CREATE TABLE IF NOT EXISTS stok.recete (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT,
    ad VARCHAR(150) NOT NULL,
    urun_id BIGINT,
    aciklama VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_recete_sirket ON stok.recete (sirket_id);

CREATE TABLE IF NOT EXISTS stok.recete_kalem (
    id BIGSERIAL PRIMARY KEY,
    recete_id BIGINT NOT NULL,
    hammadde_id BIGINT NOT NULL,
    miktar NUMERIC(19,2) NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_recete_kalem_recete ON stok.recete_kalem (recete_id);

CREATE TABLE IF NOT EXISTS stok.uretim_emri (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT,
    urun_id BIGINT NOT NULL,
    miktar NUMERIC(19,2) NOT NULL,
    durum VARCHAR(20) NOT NULL DEFAULT 'TASLAK',
    aciklama VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now(),
    tamamlanma_tarihi TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_uretim_emri_sirket ON stok.uretim_emri (sirket_id);

CREATE TABLE IF NOT EXISTS stok.stok_duzeltme (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT,
    stok_id BIGINT NOT NULL,
    stok_ad VARCHAR(255),
    eski_miktar NUMERIC(19,2),
    yeni_miktar NUMERIC(19,2),
    neden VARCHAR(500),
    kullanici_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_stok_duzeltme_sirket ON stok.stok_duzeltme (sirket_id);
