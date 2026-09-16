-- V90__teklif_revizyon_unique.sql
-- Sorun: V53, ticaret.teklif uzerinde (sirket_id, teklif_no) benzersiz index
-- (uk_teklif_no_sirket) kurmustu. Ancak revizyon akisi (TeklifService.revizyonOlustur)
-- AYNI teklif_no ile yeni bir satir (revizyon_no + 1) olusturur; bu index yuzunden
-- ikinci revizyon DataIntegrityViolationException ile patlar.
-- Cozum: benzersizligi (sirket_id, teklif_no, revizyon_no) uclusune tasimak.

-- 1) revizyon_no NULL ise 0'a cek (unique index NULL'lari ayri deger sayar)
UPDATE ticaret.teklif SET revizyon_no = 0 WHERE revizyon_no IS NULL;

-- 2) Eski (sirket, teklif_no) benzersiz index'ini kaldir
DROP INDEX IF EXISTS ticaret.uk_teklif_no_sirket;

-- 3) Ayni (sirket, teklif_no, revizyon_no) uclusunde tekrar varsa en yenisini (buyuk id) birak
DELETE FROM ticaret.teklif a
USING ticaret.teklif b
WHERE a.id < b.id
  AND a.sirket_id IS NOT DISTINCT FROM b.sirket_id
  AND a.teklif_no = b.teklif_no
  AND COALESCE(a.revizyon_no, 0) = COALESCE(b.revizyon_no, 0);

-- 4) Yeni bilesik benzersiz index
CREATE UNIQUE INDEX IF NOT EXISTS uk_teklif_no_sirket_rev
    ON ticaret.teklif (sirket_id, teklif_no, revizyon_no);
