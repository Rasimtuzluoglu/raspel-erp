-- V174__dashboard_tercih.sql
-- Anasayfa kisisellestirme: kullanici bazli widget duzeni.
--
-- Amac:
--   - Kullanicinin surukle-birak ile olusturdugu Anasayfa duzenini (JSON)
--     sunucuda saklamak. Boylece düzen tarayici degisse de korunur.
--   - `duzen` en fazla 20 KB ve gecerli bir JSON object olmalidir; dogrulama
--     servis katmaninda (Jackson readTree) yapilir.
--
-- Notlar:
--   - kullanici_id UNIQUE: bir kullanicinin tek duzeni olur.
--   - sirket_id tenant izolasyonu icin tutulur; FK mevcut konvansiyon geregi
--     eklenmez (V172 onay_karar ile ayni yaklasim).
--   - Tercih cache'lenmez (kullaniciya ozel ve sik degisen veri).
--
-- Idempotent: tablo ve indeks olusturmalari IF NOT EXISTS ile korunur.
-- =====================================================================

CREATE TABLE IF NOT EXISTS sistem.dashboard_tercih (
    id                BIGSERIAL PRIMARY KEY,
    kullanici_id      BIGINT    NOT NULL,
    sirket_id         BIGINT    NOT NULL,
    duzen             TEXT      NOT NULL, -- JSON object
    guncelleme_tarihi TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Bir kullanicinin tek duzeni (upsert anahtari).
CREATE UNIQUE INDEX IF NOT EXISTS uk_dashboard_tercih_kullanici
    ON sistem.dashboard_tercih (kullanici_id);

-- Sirket bazli okuma/temizlik sorgulari icin.
CREATE INDEX IF NOT EXISTS idx_dashboard_tercih_sirket
    ON sistem.dashboard_tercih (sirket_id);
