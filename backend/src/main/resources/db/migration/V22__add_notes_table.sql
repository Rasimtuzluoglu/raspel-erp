CREATE SCHEMA IF NOT EXISTS sistem;

CREATE TABLE IF NOT EXISTS sistem.notlar (
    id BIGSERIAL PRIMARY KEY,
    baslik VARCHAR(200) NOT NULL,
    icerik TEXT,
    onem_derecesi VARCHAR(20) DEFAULT 'NORMAL',
    renk VARCHAR(20) DEFAULT 'MAVI',
    kullanici_id BIGINT,
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL,
    guncelleme_tarihi TIMESTAMP
);
