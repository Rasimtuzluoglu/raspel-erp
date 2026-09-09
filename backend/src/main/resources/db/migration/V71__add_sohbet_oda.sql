-- V71__add_sohbet_oda.sql
-- Grup sohbet odaları ve üyelikleri.
CREATE TABLE IF NOT EXISTS sistem.sohbet_oda (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT,
    ad VARCHAR(100) NOT NULL,
    aciklama VARCHAR(300),
    olusturan_kullanici_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sohbet_oda_sirket ON sistem.sohbet_oda (sirket_id);

CREATE TABLE IF NOT EXISTS sistem.sohbet_oda_uye (
    id BIGSERIAL PRIMARY KEY,
    oda_id BIGINT NOT NULL,
    kullanici_id BIGINT NOT NULL,
    eklenme_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sohbet_oda_uye_oda ON sistem.sohbet_oda_uye (oda_id);
CREATE INDEX IF NOT EXISTS idx_sohbet_oda_uye_kullanici ON sistem.sohbet_oda_uye (kullanici_id);

-- Oda bazlı mesajlar için oda_id kolonu (NULL = genel şirket sohbeti).
ALTER TABLE sistem.sohbet_mesaj ADD COLUMN IF NOT EXISTS oda_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_sohbet_mesaj_oda ON sistem.sohbet_mesaj (oda_id, olusturma_tarihi);
