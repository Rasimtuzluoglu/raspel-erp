-- Satın alma tablolarında tenant (sirket_id) NOT NULL + FK.
-- Geçmişte create sırasında sirket_id hiç set edilmediği için NULL satırlar
-- oluşabiliyordu; bu kayıtlar listeleme filtresinde görünmüyordu ("kayıt yaptım
-- ama görünmüyor" hatası). Önce backfill, sonra NOT NULL.

-- 1) Backfill: tek firmalı kurulumda NULL kayıtlar o firmaya bağlanır; çok firmalı
--    ortamda ise (belirsiz tenant) bu yetim kayıtlar temizlenir.
DO $$
DECLARE
    sirket_sayisi int;
    tek_sirket bigint;
BEGIN
    SELECT count(*), min(id) INTO sirket_sayisi, tek_sirket FROM sistem.sirket;
    IF sirket_sayisi = 1 THEN
        UPDATE satinalma.satinalma_talep SET sirket_id = tek_sirket WHERE sirket_id IS NULL;
        UPDATE satinalma.satinalma_siparis SET sirket_id = tek_sirket WHERE sirket_id IS NULL;
    ELSE
        DELETE FROM satinalma.satinalma_talep WHERE sirket_id IS NULL;
        DELETE FROM satinalma.satinalma_siparis WHERE sirket_id IS NULL;
    END IF;
END $$;

-- 2) Tenant FK (yoksa; V122 zaten eklemiş olabilir) + NOT NULL
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_satinalma_talep_sirket') THEN
        ALTER TABLE satinalma.satinalma_talep ADD CONSTRAINT fk_satinalma_talep_sirket
            FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
    END IF;
END $$;
ALTER TABLE satinalma.satinalma_talep VALIDATE CONSTRAINT fk_satinalma_talep_sirket;
ALTER TABLE satinalma.satinalma_talep ALTER COLUMN sirket_id SET NOT NULL;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_satinalma_siparis_sirket') THEN
        ALTER TABLE satinalma.satinalma_siparis ADD CONSTRAINT fk_satinalma_siparis_sirket
            FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
    END IF;
END $$;
ALTER TABLE satinalma.satinalma_siparis VALIDATE CONSTRAINT fk_satinalma_siparis_sirket;
ALTER TABLE satinalma.satinalma_siparis ALTER COLUMN sirket_id SET NOT NULL;
