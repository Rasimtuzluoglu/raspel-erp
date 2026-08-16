-- V47__add_hata_log.sql
-- Sunucu hata kayıtları (müşterinin hataları tespit edip müdahale edebilmesi için).
CREATE TABLE IF NOT EXISTS sistem.hata_log (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT,
    tur VARCHAR(200),
    mesaj TEXT,
    endpoint VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_hata_log_tarih ON sistem.hata_log(olusturma_tarihi);
