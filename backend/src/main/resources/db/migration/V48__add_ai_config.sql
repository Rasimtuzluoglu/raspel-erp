CREATE TABLE IF NOT EXISTS sistem.ai_config (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    encrypted_api_key TEXT NOT NULL,
    model VARCHAR(50),
    aktif BOOLEAN DEFAULT true,
    olusturma_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    guncelleme_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_config_sirket UNIQUE (sirket_id)
);
