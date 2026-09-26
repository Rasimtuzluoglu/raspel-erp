-- Bordro onay/kilit alanları. ONAYLANDI sonrası bordro düzenlenemez/silinemez; onay
-- bilgisi denetim için saklanır.
ALTER TABLE ik.maas_bordro ADD COLUMN IF NOT EXISTS durum VARCHAR(20) NOT NULL DEFAULT 'TASLAK';
ALTER TABLE ik.maas_bordro ADD COLUMN IF NOT EXISTS onay_tarihi TIMESTAMP;
ALTER TABLE ik.maas_bordro ADD COLUMN IF NOT EXISTS onaylayan VARCHAR(100);
