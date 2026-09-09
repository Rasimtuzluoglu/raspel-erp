-- V77__add_pos_terminali.sql
-- POS terminalleri (kredi karti tek cekim takibi) ve hareket uzerindeki POS/komisyon/valor alanlari.
CREATE TABLE IF NOT EXISTS muhasebe.pos_terminali (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT,
    ad VARCHAR(150) NOT NULL,
    banka_id BIGINT,
    komisyon_orani NUMERIC(5,2) NOT NULL DEFAULT 0,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_pos_terminali_sirket ON muhasebe.pos_terminali (sirket_id);

ALTER TABLE cari.hareket ADD COLUMN IF NOT EXISTS pos_terminali_id BIGINT;
ALTER TABLE cari.hareket ADD COLUMN IF NOT EXISTS pos_ad VARCHAR(150);
ALTER TABLE cari.hareket ADD COLUMN IF NOT EXISTS komisyon_tutar NUMERIC(19,2);
ALTER TABLE cari.hareket ADD COLUMN IF NOT EXISTS valor_tarihi DATE;
