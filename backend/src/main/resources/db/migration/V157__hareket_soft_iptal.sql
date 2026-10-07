-- =====================================================================
-- V157: Cari hareketlerde soft iptal (Faz 2.6)
--
-- DENETIM BULGUSU:
--   Hareket silme sert silmeydi; iptal işlemi denetim izi bırakmıyordu.
--
-- COZUM:
--   `cari.hareket.iptal` bayrağı. Uygulama tarafında @SQLRestriction ile
--   iptal kayıtlar sorgulardan otomatik hariç tutulur; bakiye/kasa/banka/fatura
--   etkileri iptal anında geri alınır. Kayıt denetim için saklanır.
--
-- Idempotent.
-- =====================================================================

ALTER TABLE cari.hareket
    ADD COLUMN IF NOT EXISTS iptal boolean NOT NULL DEFAULT false;

ALTER TABLE cari.hareket
    ADD COLUMN IF NOT EXISTS iptal_tarihi timestamp;

CREATE INDEX IF NOT EXISTS idx_hareket_iptal ON cari.hareket (sirket_id, iptal);
