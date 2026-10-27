-- =====================================================================
-- V169: İrsaliye sevkiyat akışı + teslimat bağlantısı
--
-- Durumlar: TASLAK, HAZIRLANDI, SEVK_EDILDI, TESLIM_EDILDI, IPTAL
-- Stok yalnızca SEVK_EDILDI geçişinde hareket eder.
-- Mevcut KESILDI kayıtları (stokları zaten işlenmiş) SEVK_EDILDI'ya taşınır.
-- Idempotent.
-- =====================================================================

ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS plaka VARCHAR(20);
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS sofor_id BIGINT;
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS teslim_alan VARCHAR(150);
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS teslim_tarihi TIMESTAMP;
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS teslimat_adresi VARCHAR(500);
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS koli_adeti INTEGER;
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS desi NUMERIC(10, 2);
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS brut_kg NUMERIC(10, 2);
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS tasiyici_firma VARCHAR(150);
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS sevk_tarihi TIMESTAMP;
ALTER TABLE muhasebe.irsaliye ADD COLUMN IF NOT EXISTS teslimat_id BIGINT;

ALTER TABLE muhasebe.irsaliye DROP CONSTRAINT IF EXISTS fk_irsaliye_teslimat;
ALTER TABLE muhasebe.irsaliye
    ADD CONSTRAINT fk_irsaliye_teslimat FOREIGN KEY (teslimat_id)
    REFERENCES ticaret.teslimat (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS ix_irsaliye_sirket_durum ON muhasebe.irsaliye (sirket_id, durum);

CREATE TABLE IF NOT EXISTS muhasebe.irsaliye_durum_log (
    id                BIGSERIAL PRIMARY KEY,
    irsaliye_id       BIGINT       NOT NULL,
    sirket_id         BIGINT       NOT NULL,
    eski_durum        VARCHAR(20),
    yeni_durum        VARCHAR(20)  NOT NULL,
    islem             VARCHAR(20)  NOT NULL,
    aciklama          VARCHAR(500),
    kullanici_id      BIGINT,
    olusturma_tarihi  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_irsaliye_log_irsaliye FOREIGN KEY (irsaliye_id)
        REFERENCES muhasebe.irsaliye (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_irsaliye_log_irsaliye
    ON muhasebe.irsaliye_durum_log (irsaliye_id, olusturma_tarihi);

ALTER TABLE ticaret.teslimat ADD COLUMN IF NOT EXISTS irsaliye_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_teslimat_irsaliye
    ON ticaret.teslimat (irsaliye_id) WHERE irsaliye_id IS NOT NULL;

UPDATE muhasebe.irsaliye
SET durum = 'SEVK_EDILDI',
    sevk_tarihi = COALESCE(sevk_tarihi, tarih::timestamp)
WHERE durum = 'KESILDI';
