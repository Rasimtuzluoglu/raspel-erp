-- V104__sifre_sifirla.sql
-- Kullaniciya e-posta adresi (sifre sifirlama bagi icin) ve tek kullanimlik
-- sifre sifirlama token tablosu. Token'lar hash'lenerek saklanir.

ALTER TABLE sistem.kullanici ADD COLUMN IF NOT EXISTS email VARCHAR(255);

CREATE TABLE IF NOT EXISTS sistem.sifre_sifirla_token (
    id            BIGSERIAL PRIMARY KEY,
    kullanici_id  BIGINT NOT NULL,
    token_hash    VARCHAR(128) NOT NULL UNIQUE,
    son_kullanma  TIMESTAMP NOT NULL,
    kullanildi    BOOLEAN NOT NULL DEFAULT false,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_sifre_token_kullanici FOREIGN KEY (kullanici_id)
        REFERENCES sistem.kullanici(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sifre_token_kullanici
    ON sistem.sifre_sifirla_token(kullanici_id);

CREATE INDEX IF NOT EXISTS idx_sifre_token_son_kullanma
    ON sistem.sifre_sifirla_token(son_kullanma);
