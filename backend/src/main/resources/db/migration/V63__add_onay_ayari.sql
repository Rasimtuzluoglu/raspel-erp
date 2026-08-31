-- V63__add_onay_ayari.sql
-- Yapılandırılabilir onay eşikleri (iş akışı matrisi).
CREATE TABLE IF NOT EXISTS sistem.onay_ayari (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT NOT NULL,
    modul VARCHAR(50) NOT NULL,        -- MASRAF, SATINALMA, IZIN
    esik_tutar NUMERIC(19,2) NOT NULL DEFAULT 0,
    otomatik_onay BOOLEAN NOT NULL DEFAULT FALSE,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (sirket_id, modul)
);
