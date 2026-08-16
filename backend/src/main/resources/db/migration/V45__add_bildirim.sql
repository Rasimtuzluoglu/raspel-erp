-- V45__add_bildirim.sql
-- Kalıcı bildirimler (bildirim merkezi geçmişi).
CREATE TABLE IF NOT EXISTS sistem.bildirim (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT,
    tur VARCHAR(20),
    baslik VARCHAR(200),
    mesaj TEXT,
    okundu BOOLEAN NOT NULL DEFAULT false,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bildirim_sirket ON sistem.bildirim(sirket_id, olusturma_tarihi);
