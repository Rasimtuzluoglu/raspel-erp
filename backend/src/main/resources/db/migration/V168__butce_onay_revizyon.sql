-- =====================================================================
-- V168: Bütçe onay akışı ve revizyon geçmişi
--
-- 1) Bütçe kaydına durum (TASLAK / ONAYLANDI / REDDEDILDI), revizyon
--    numarası ve onay bilgileri eklenir.
-- 2) Her güncelleme/onay/red işlemi butce_revizyon tablosuna snapshot yazar.
--
-- Mevcut kayıtlar onaylı kabul edilir (raporlar bozulmaz).
-- Idempotent.
-- =====================================================================

ALTER TABLE finans.butce ADD COLUMN IF NOT EXISTS durum VARCHAR(20) NOT NULL DEFAULT 'ONAYLANDI';
ALTER TABLE finans.butce ADD COLUMN IF NOT EXISTS revizyon INTEGER NOT NULL DEFAULT 1;
ALTER TABLE finans.butce ADD COLUMN IF NOT EXISTS onaylayan_kullanici_id BIGINT;
ALTER TABLE finans.butce ADD COLUMN IF NOT EXISTS onay_tarihi TIMESTAMP;
ALTER TABLE finans.butce ADD COLUMN IF NOT EXISTS red_gerekcesi VARCHAR(500);

CREATE TABLE IF NOT EXISTS finans.butce_revizyon (
    id                     BIGSERIAL PRIMARY KEY,
    butce_id               BIGINT       NOT NULL,
    sirket_id              BIGINT       NOT NULL,
    revizyon               INTEGER      NOT NULL,
    ad                     VARCHAR(200),
    kategori               VARCHAR(100),
    yil                    INTEGER,
    ay                     INTEGER,
    tutar                  NUMERIC(19, 2),
    tur                    VARCHAR(10),
    durum                  VARCHAR(20),
    islem                  VARCHAR(20),
    aciklama               VARCHAR(500),
    degistiren_kullanici_id BIGINT,
    olusturma_tarihi       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_butce_revizyon_butce FOREIGN KEY (butce_id)
        REFERENCES finans.butce (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_butce_revizyon_butce ON finans.butce_revizyon (butce_id, olusturma_tarihi);
CREATE INDEX IF NOT EXISTS ix_butce_sirket_durum_yil ON finans.butce (sirket_id, durum, yil);
