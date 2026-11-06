-- Passkey / WebAuthn kimlik bilgileri.
-- Her kayit bir kullaniciya aittir; credential_id global benzersizdir.

CREATE TABLE IF NOT EXISTS sistem.passkey (
    id BIGSERIAL PRIMARY KEY,
    kullanici_id BIGINT NOT NULL REFERENCES sistem.kullanici(id) ON DELETE CASCADE,
    credential_id VARCHAR(255) NOT NULL,
    etiket VARCHAR(100) NOT NULL,
    attested_credential_data TEXT NOT NULL,
    counter BIGINT NOT NULL DEFAULT 0,
    uv_initialized BOOLEAN NOT NULL DEFAULT TRUE,
    backup_eligible BOOLEAN,
    backed_up BOOLEAN,
    transports VARCHAR(255),
    user_handle VARCHAR(255),
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT NOW(),
    son_kullanma_tarihi TIMESTAMP,
    CONSTRAINT ux_passkey_credential UNIQUE (credential_id)
);

CREATE INDEX IF NOT EXISTS ix_passkey_kullanici ON sistem.passkey (kullanici_id);
