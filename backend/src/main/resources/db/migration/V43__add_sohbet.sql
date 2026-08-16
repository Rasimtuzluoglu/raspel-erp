-- V43__add_sohbet.sql
-- Ekip içi sohbet mesajları.
CREATE TABLE IF NOT EXISTS sistem.sohbet_mesaj (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT,
    kullanici_id BIGINT,
    kullanici_adi VARCHAR(100),
    mesaj TEXT NOT NULL,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sohbet_sirket_tarih ON sistem.sohbet_mesaj(sirket_id, olusturma_tarihi);
