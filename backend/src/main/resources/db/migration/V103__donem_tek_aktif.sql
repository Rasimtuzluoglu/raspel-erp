-- V103__donem_tek_aktif.sql
-- Ayni sirkette yalnizca bir aktif donem olabilir.
-- Once olasi coklu aktifleri tek aktife indir (en yeni baslangic, esitlikte en buyuk id kalir).
UPDATE sistem.donem d SET aktif = false
 WHERE d.aktif = true
   AND EXISTS (
       SELECT 1 FROM sistem.donem d2
        WHERE d2.sirket_id = d.sirket_id
          AND d2.aktif = true
          AND (d2.baslangic > d.baslangic
               OR (d2.baslangic = d.baslangic AND d2.id > d.id))
   );

CREATE UNIQUE INDEX IF NOT EXISTS uq_donem_tek_aktif
    ON sistem.donem(sirket_id)
    WHERE aktif = true;
