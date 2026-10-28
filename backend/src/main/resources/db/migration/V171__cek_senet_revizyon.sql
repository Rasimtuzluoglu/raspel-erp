-- =====================================================================
-- V171: Çek/Senet revizyonu
--
-- Amaç:
--   - Tahsilatta oluşan cari/kasa/banka hareketlerini çek/senet kaydına
--     bağlamak (tahsil iptalinde ters kayıt için id'lerin kaybolmaması).
--   - Ciro / teminat / protesto yaşam döngüsü alanlarını eklemek.
--   - Durum geçişlerini denetlenebilir kılmak için durum logu tutmak.
--
-- Idempotent: tüm kolon/tablo/indeks oluşturmaları IF NOT EXISTS,
-- kısıt eklemeleri pg_constraint kontrolü ile korunur.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) muhasebe.cek_senet yeni alanlar
-- ---------------------------------------------------------------------
ALTER TABLE muhasebe.cek_senet ADD COLUMN IF NOT EXISTS tahsilat_hareket_id BIGINT;
ALTER TABLE muhasebe.cek_senet ADD COLUMN IF NOT EXISTS kasa_hareket_id BIGINT;
ALTER TABLE muhasebe.cek_senet ADD COLUMN IF NOT EXISTS banka_hareketi_id BIGINT;
ALTER TABLE muhasebe.cek_senet ADD COLUMN IF NOT EXISTS ciro_cari_id BIGINT;
ALTER TABLE muhasebe.cek_senet ADD COLUMN IF NOT EXISTS ciro_tarihi TIMESTAMP;
ALTER TABLE muhasebe.cek_senet ADD COLUMN IF NOT EXISTS ciro_notu VARCHAR(500);
ALTER TABLE muhasebe.cek_senet ADD COLUMN IF NOT EXISTS teminat_tarihi TIMESTAMP;
ALTER TABLE muhasebe.cek_senet ADD COLUMN IF NOT EXISTS teminat_aciklama VARCHAR(500);
ALTER TABLE muhasebe.cek_senet ADD COLUMN IF NOT EXISTS protesto_tarihi TIMESTAMP;

-- Tahsil iptalinde ters kaydın bulunabilmesi için bağ id'leri indekslenir.
CREATE INDEX IF NOT EXISTS idx_cek_senet_tahsilat_hareket
    ON muhasebe.cek_senet (tahsilat_hareket_id);
CREATE INDEX IF NOT EXISTS idx_cek_senet_kasa_hareket
    ON muhasebe.cek_senet (kasa_hareket_id);
CREATE INDEX IF NOT EXISTS idx_cek_senet_banka_hareketi
    ON muhasebe.cek_senet (banka_hareketi_id);
CREATE INDEX IF NOT EXISTS idx_cek_senet_ciro_cari
    ON muhasebe.cek_senet (ciro_cari_id);

-- Sık kullanılan liste/sıralama ve KPI sorguları (şirket + vade + durum).
CREATE INDEX IF NOT EXISTS idx_cek_senet_sirket_vade_durum
    ON muhasebe.cek_senet (sirket_id, vade_tarihi, durum);

-- ---------------------------------------------------------------------
-- 2) Durum geçmişi
--
--    cek_senet silinirse logları da silinir (ON DELETE CASCADE); böylece
--    silinemez durumdaki kayıtların logu da kalıcı çöp bırakmaz.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS muhasebe.cek_senet_durum_log (
    id               BIGSERIAL PRIMARY KEY,
    cek_senet_id     BIGINT       NOT NULL,
    sirket_id        BIGINT,
    eski_durum       VARCHAR(30),
    yeni_durum       VARCHAR(30),
    islem            VARCHAR(30)  NOT NULL,
    kullanici_id     BIGINT,
    aciklama         VARCHAR(500),
    olusturma_tarihi TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cek_senet_durum_log_cek
    ON muhasebe.cek_senet_durum_log (cek_senet_id, olusturma_tarihi, id);
CREATE INDEX IF NOT EXISTS idx_cek_senet_durum_log_sirket
    ON muhasebe.cek_senet_durum_log (sirket_id, olusturma_tarihi DESC);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_cek_senet_durum_log_cek') THEN
        ALTER TABLE muhasebe.cek_senet_durum_log
            ADD CONSTRAINT fk_cek_senet_durum_log_cek
            FOREIGN KEY (cek_senet_id) REFERENCES muhasebe.cek_senet (id)
            ON DELETE CASCADE;
    END IF;
END $$;
