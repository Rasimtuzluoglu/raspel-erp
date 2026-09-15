-- V89__uretim_detay.sql
-- Üretim modülü detaylandırma: emir planlama/fire/maliyet, reçete revizyon/fire, durum logu.

ALTER TABLE stok.uretim_emri
    ADD COLUMN IF NOT EXISTS planlanan_baslangic DATE,
    ADD COLUMN IF NOT EXISTS planlanan_bitis DATE,
    ADD COLUMN IF NOT EXISTS baslama_tarihi TIMESTAMP,
    ADD COLUMN IF NOT EXISTS oncelik VARCHAR(10) DEFAULT 'NORMAL',
    ADD COLUMN IF NOT EXISTS uretilen_miktar NUMERIC(19,2),
    ADD COLUMN IF NOT EXISTS fire_miktar NUMERIC(19,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sorumlu_personel_id BIGINT,
    ADD COLUMN IF NOT EXISTS depo_id BIGINT,
    ADD COLUMN IF NOT EXISTS hammadde_maliyeti NUMERIC(19,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS iscilik_maliyeti NUMERIC(19,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS toplam_maliyet NUMERIC(19,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS notlar TEXT;

ALTER TABLE stok.recete
    ADD COLUMN IF NOT EXISTS aktif BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS revizyon INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS fire_orani NUMERIC(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS notlar TEXT;

ALTER TABLE stok.recete_kalem
    ADD COLUMN IF NOT EXISTS birim VARCHAR(20),
    ADD COLUMN IF NOT EXISTS fire_orani NUMERIC(5,2) DEFAULT 0;

CREATE TABLE IF NOT EXISTS stok.uretim_emri_log (
    id BIGSERIAL PRIMARY KEY,
    uretim_emri_id BIGINT NOT NULL,
    onceki_durum VARCHAR(20),
    yeni_durum VARCHAR(20),
    kullanici_id BIGINT,
    aciklama VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_uretim_emri_log_emri ON stok.uretim_emri_log (uretim_emri_id);
