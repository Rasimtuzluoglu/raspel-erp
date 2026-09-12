-- V85__add_push_abonelik.sql
-- PWA web push abonelikleri (tarayici endpoint + sifreleme anahtarlari).
CREATE TABLE IF NOT EXISTS sistem.push_abonelik (
    id BIGSERIAL PRIMARY KEY,
    kullanici_id BIGINT,
    sirket_id BIGINT,
    endpoint TEXT NOT NULL,
    p256dh VARCHAR(512) NOT NULL,
    auth VARCHAR(512) NOT NULL,
    user_agent VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_push_abonelik_endpoint ON sistem.push_abonelik (endpoint);
CREATE INDEX IF NOT EXISTS idx_push_abonelik_sirket ON sistem.push_abonelik (sirket_id);
CREATE INDEX IF NOT EXISTS idx_push_abonelik_kullanici ON sistem.push_abonelik (kullanici_id);
