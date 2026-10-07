-- =====================================================================
-- V160: Sohbet oda üyelik rolleri (Faz 3.2)
--
-- OWNER / ADMIN / MEMBER. Oda kurucusu OWNER; mevcut kayıtlar MEMBER olarak
-- işaretlenir (kurucu satırı OWNER'a yükseltilir).
--
-- Idempotent.
-- =====================================================================

ALTER TABLE sistem.sohbet_oda_uye
    ADD COLUMN IF NOT EXISTS rol varchar(20) DEFAULT 'MEMBER';

UPDATE sistem.sohbet_oda_uye SET rol = 'MEMBER' WHERE rol IS NULL;

-- Oda kurucusunu OWNER yap.
UPDATE sistem.sohbet_oda_uye u
SET rol = 'OWNER'
FROM sistem.sohbet_oda o
WHERE u.oda_id = o.id
  AND o.olusturan_kullanici_id IS NOT NULL
  AND u.kullanici_id = o.olusturan_kullanici_id
  AND u.rol <> 'OWNER';
