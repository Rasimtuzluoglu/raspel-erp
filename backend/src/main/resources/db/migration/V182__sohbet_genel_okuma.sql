-- V182: genel sohbet okuma durumu (Faz 9).
--
-- Okunmamis rozeti tarayici localStorage'ina bagliydi; kullanici baska bir
-- cihazdan girdiginde tum gecmis okunmamis gorunuyor ya da rozet kayboluyordu.
-- Okuma isareti artik kullanici bazinda sunucuda tutulur.
CREATE TABLE IF NOT EXISTS sistem.sohbet_genel_okuma (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT NOT NULL,
    kullanici_id BIGINT NOT NULL,
    son_okunan_id BIGINT NOT NULL DEFAULT 0,
    guncelleme_tarihi TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_sohbet_genel_okuma UNIQUE (sirket_id, kullanici_id)
);

CREATE INDEX IF NOT EXISTS idx_sohbet_genel_okuma_kullanici
    ON sistem.sohbet_genel_okuma (kullanici_id);
