-- Giris ekraninda istege bagli sirket kodu.
-- Mevcut sirketler icin addan benzersiz bir slug uretilir; yonetici sonradan
-- Sirketler ekranindan degistirebilir.

ALTER TABLE sistem.sirket ADD COLUMN IF NOT EXISTS kod VARCHAR(60);

UPDATE sistem.sirket
SET kod = NULLIF(
        trim(both '-' from regexp_replace(
            lower(translate(ad, 'çğıöşüÇĞİÖŞÜ', 'cgiosuCGIOSU')),
            '[^a-z0-9]+', '-', 'g'
        )),
        ''
    )
WHERE kod IS NULL;

UPDATE sistem.sirket
SET kod = 'sirket-' || id
WHERE kod IS NULL OR kod = '';

WITH tekrar AS (
    SELECT id, kod,
           row_number() OVER (PARTITION BY lower(kod) ORDER BY id) AS rn
    FROM sistem.sirket
)
UPDATE sistem.sirket s
SET kod = tekrar.kod || '-' || s.id
FROM tekrar
WHERE s.id = tekrar.id AND tekrar.rn > 1;

ALTER TABLE sistem.sirket ALTER COLUMN kod SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_sirket_kod ON sistem.sirket (lower(kod));
