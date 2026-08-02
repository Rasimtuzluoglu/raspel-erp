CREATE SCHEMA IF NOT EXISTS sistem;

CREATE TABLE IF NOT EXISTS sistem.belge (
    id BIGSERIAL PRIMARY KEY,
    entity_adi VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    dosya_adi VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP NOT NULL
);
