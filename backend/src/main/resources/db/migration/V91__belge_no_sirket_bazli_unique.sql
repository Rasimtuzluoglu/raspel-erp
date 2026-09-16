-- V91__belge_no_sirket_bazli_unique.sql
-- Sorun: belge numaralari (fatura/siparis/talep/irsaliye) V1'de GLOBAL unique
-- tanimlanmisti. Multi-tenant kullanimda bir sirketin numarasi diger sirketi
-- bloklar (teklif_no icin V53/V86'da ayni sorun cozulmustu). Cozum: benzersizligi
-- (sirket_id, belge_no) bilesik index'e tasimak.

-- 1) Bu kolonlar uzerindeki mevcut UNIQUE kisitlarini kaldir (isim otomatik uretilmis olabilir)
DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN
    SELECT con.conname AS conname, ns.nspname AS schema, rel.relname AS tablo
    FROM pg_constraint con
    JOIN pg_class rel ON rel.oid = con.conrelid
    JOIN pg_namespace ns ON ns.oid = rel.relnamespace
    WHERE con.contype = 'u'
      AND (
        (ns.nspname = 'fatura' AND rel.relname = 'fatura' AND con.conname LIKE '%fatura_numarasi%') OR
        (ns.nspname = 'siparis' AND rel.relname = 'siparis' AND con.conname LIKE '%siparis_no%') OR
        (ns.nspname = 'satinalma' AND rel.relname = 'satinalma_talep' AND con.conname LIKE '%talep_no%') OR
        (ns.nspname = 'satinalma' AND rel.relname = 'satinalma_siparis' AND con.conname LIKE '%siparis_no%') OR
        (ns.nspname = 'muhasebe' AND rel.relname = 'irsaliye' AND con.conname LIKE '%irsaliye_no%')
      )
  LOOP
    EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT %I', r.schema, r.tablo, r.conname);
  END LOOP;
END $$;

-- 2) Veri temizligi: sirket icinde mukerrer belge no varsa en yenisini (buyuk id) birak
DELETE FROM fatura.fatura a USING fatura.fatura b
WHERE a.id < b.id AND a.sirket_id IS NOT DISTINCT FROM b.sirket_id AND a.fatura_numarasi = b.fatura_numarasi;

DELETE FROM siparis.siparis a USING siparis.siparis b
WHERE a.id < b.id AND a.sirket_id IS NOT DISTINCT FROM b.sirket_id AND a.siparis_no = b.siparis_no;

DELETE FROM satinalma.satinalma_talep a USING satinalma.satinalma_talep b
WHERE a.id < b.id AND a.sirket_id IS NOT DISTINCT FROM b.sirket_id AND a.talep_no = b.talep_no;

DELETE FROM satinalma.satinalma_siparis a USING satinalma.satinalma_siparis b
WHERE a.id < b.id AND a.sirket_id IS NOT DISTINCT FROM b.sirket_id AND a.siparis_no = b.siparis_no;

DELETE FROM muhasebe.irsaliye a USING muhasebe.irsaliye b
WHERE a.id < b.id AND a.sirket_id IS NOT DISTINCT FROM b.sirket_id AND a.irsaliye_no = b.irsaliye_no;

-- 3) Bilesik benzersiz indexler (sirket bazli)
CREATE UNIQUE INDEX IF NOT EXISTS uk_fatura_no_sirket ON fatura.fatura(sirket_id, fatura_numarasi);
CREATE UNIQUE INDEX IF NOT EXISTS uk_siparis_no_sirket ON siparis.siparis(sirket_id, siparis_no);
CREATE UNIQUE INDEX IF NOT EXISTS uk_satinalma_talep_no_sirket ON satinalma.satinalma_talep(sirket_id, talep_no);
CREATE UNIQUE INDEX IF NOT EXISTS uk_satinalma_siparis_no_sirket ON satinalma.satinalma_siparis(sirket_id, siparis_no);
CREATE UNIQUE INDEX IF NOT EXISTS uk_irsaliye_no_sirket ON muhasebe.irsaliye(sirket_id, irsaliye_no);
