-- V62__add_api_token.sql
-- Kişisel erişim token'ları (üçüncü taraf entegrasyonlar / REST API için).
CREATE TABLE IF NOT EXISTS sistem.api_token (
    id BIGSERIAL PRIMARY KEY,
    kullanici_id BIGINT NOT NULL REFERENCES sistem.kullanici(id) ON DELETE CASCADE,
    ad VARCHAR(100) NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    son_kullanim TIMESTAMP,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);
