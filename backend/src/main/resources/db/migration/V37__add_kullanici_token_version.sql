-- V37__add_kullanici_token_version.sql
ALTER TABLE sistem.kullanici ADD COLUMN IF NOT EXISTS token_version BIGINT NOT NULL DEFAULT 0;
