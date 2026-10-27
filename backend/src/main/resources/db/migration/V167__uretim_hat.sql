-- =====================================================================
-- V167: Üretim hatları / iş merkezleri
--
-- Üretim emirleri bir hatta (iş merkezine) atanabilir. Yönetim görünümünde
-- hat doluluğu ve geciken emir sayısı izlenir.
--
-- Idempotent.
-- =====================================================================

CREATE TABLE IF NOT EXISTS stok.uretim_hat (
    id                BIGSERIAL PRIMARY KEY,
    sirket_id         BIGINT       NOT NULL,
    ad                VARCHAR(120) NOT NULL,
    aciklama          VARCHAR(500),
    kapasite          INTEGER      NOT NULL DEFAULT 5,
    aktif             BOOLEAN      NOT NULL DEFAULT TRUE,
    olusturma_tarihi  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_uretim_hat_sirket_ad
    ON stok.uretim_hat (sirket_id, LOWER(ad));

ALTER TABLE stok.uretim_emri ADD COLUMN IF NOT EXISTS hat_id BIGINT;

ALTER TABLE stok.uretim_emri DROP CONSTRAINT IF EXISTS fk_uretim_emri_hat;
ALTER TABLE stok.uretim_emri
    ADD CONSTRAINT fk_uretim_emri_hat
    FOREIGN KEY (hat_id) REFERENCES stok.uretim_hat (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS ix_uretim_emri_hat ON stok.uretim_emri (hat_id);
