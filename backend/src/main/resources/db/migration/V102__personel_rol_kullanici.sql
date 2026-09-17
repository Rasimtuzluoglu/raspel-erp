-- V100__personel_rol_kullanici.sql
-- Personel gorev/rol tanimi ve opsiyonel kullanici hesabi bagi.
-- rol: SOFOR | DEPOCU | DIGER (bos ise tanimsiz). Sofor personelin bagli kullanici
-- hesabi (role=DRIVER) teslimat sofor seciminde listelenir.
ALTER TABLE personel.personel ADD COLUMN IF NOT EXISTS rol VARCHAR(20);
ALTER TABLE personel.personel ADD COLUMN IF NOT EXISTS kullanici_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_personel_rol ON personel.personel(sirket_id, rol);
