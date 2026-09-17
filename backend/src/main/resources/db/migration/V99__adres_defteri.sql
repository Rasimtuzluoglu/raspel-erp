-- V99__adres_defteri.sql
-- Adres defteri: elektrikci, tesisatci, marangoz vb. hizmet kisileri icin rehber.
CREATE TABLE IF NOT EXISTS sistem.adres_defteri (
    id                BIGSERIAL PRIMARY KEY,
    sirket_id         BIGINT       NOT NULL,
    ad                VARCHAR(200) NOT NULL,
    tur               VARCHAR(50),
    telefon           VARCHAR(30),
    email             VARCHAR(150),
    adres             VARCHAR(500),
    etiketler         VARCHAR(500),
    notlar            TEXT,
    olusturma_tarihi  TIMESTAMP    NOT NULL DEFAULT NOW(),
    guncelleme_tarihi TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_adres_defteri_sirket ON sistem.adres_defteri(sirket_id);
CREATE INDEX IF NOT EXISTS idx_adres_defteri_tur ON sistem.adres_defteri(sirket_id, tur);
