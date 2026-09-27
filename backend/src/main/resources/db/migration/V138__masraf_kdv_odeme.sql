-- Masraf modülü: KDV ayrıştırması ve ödeme (kasa/banka) bilgisi.
-- Önceden masraf yalnızca tutar olarak tutuluyor, muhasebeye yansımıyordu.
ALTER TABLE finans.masraf ADD COLUMN IF NOT EXISTS kdv_orani NUMERIC(5,2);
ALTER TABLE finans.masraf ADD COLUMN IF NOT EXISTS kdv_tutar NUMERIC(19,2);
ALTER TABLE finans.masraf ADD COLUMN IF NOT EXISTS matrah NUMERIC(19,2);
ALTER TABLE finans.masraf ADD COLUMN IF NOT EXISTS odeme_yontemi VARCHAR(20);
ALTER TABLE finans.masraf ADD COLUMN IF NOT EXISTS kasa_id BIGINT;
ALTER TABLE finans.masraf ADD COLUMN IF NOT EXISTS banka_id BIGINT;
