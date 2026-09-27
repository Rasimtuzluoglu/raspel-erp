-- Bordro odeme durumu: onay ile odeme ayrisir. Cift odeme engellenir ve onay kaldirma
-- sirasinda kasa hareketi ters kaydedilebilir. Odeme tarihi, kasa hareketinin tarihidir.
ALTER TABLE ik.maas_bordro ADD COLUMN IF NOT EXISTS odeme_durumu VARCHAR(20) NOT NULL DEFAULT 'ODENMEDI';
ALTER TABLE ik.maas_bordro ADD COLUMN IF NOT EXISTS odeme_kasa_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_maas_bordro_odeme ON ik.maas_bordro (odeme_durumu);
