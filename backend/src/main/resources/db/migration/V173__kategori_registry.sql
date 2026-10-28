-- V173__kategori_registry.sql
-- Kategoriler revizyonu: tum kategori turleri (kasa gelir/gider, stok, masraf, butce)
-- tek bir registry'de toplanir. Legacy string kolonlar KORUNUR ve registry ile
-- senkron tutulur; baglanti icin ek olarak kategori_id kolonlari eklenir.
-- Idempotent: CREATE ... IF NOT EXISTS / ADD COLUMN IF NOT EXISTS / ON CONFLICT DO NOTHING.

-- ---------------------------------------------------------------
-- 1) Registry tablosu
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sistem.kategori (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT NOT NULL,
    kapsam VARCHAR(20) NOT NULL,
    ad VARCHAR(100) NOT NULL,
    ust_kategori_id BIGINT NULL,
    renk VARCHAR(20) NULL,
    ikon VARCHAR(60) NULL,
    sira INTEGER NOT NULL DEFAULT 0,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    aciklama VARCHAR(255) NULL,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT NOW(),
    guncelleme_tarihi TIMESTAMP NULL
);

-- Kapsam kisiti (yeniden calistirmada tekrar eklenmesin)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_kategori_kapsam'
    ) THEN
        ALTER TABLE sistem.kategori ADD CONSTRAINT ck_kategori_kapsam
            CHECK (kapsam IN ('KASA_GELIR', 'KASA_GIDER', 'STOK', 'MASRAF', 'BUTCE'));
    END IF;
END $$;

-- Self-FK (en fazla 2 seviye servis katmaninda garanti edilir)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_kategori_ust'
    ) THEN
        ALTER TABLE sistem.kategori ADD CONSTRAINT fk_kategori_ust
            FOREIGN KEY (ust_kategori_id) REFERENCES sistem.kategori(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Tenant FK
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_kategori_sirket'
    ) THEN
        ALTER TABLE sistem.kategori ADD CONSTRAINT fk_kategori_sirket
            FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id);
    END IF;
END $$;

-- Ayni sirket + kapsamda ad tekrari (case-insensitive) engellenir.
CREATE UNIQUE INDEX IF NOT EXISTS uk_kategori_sirket_kapsam_ad
    ON sistem.kategori (sirket_id, kapsam, lower(ad));

CREATE INDEX IF NOT EXISTS idx_kategori_sirket_kapsam_aktif_sira
    ON sistem.kategori (sirket_id, kapsam, aktif, sira);

CREATE INDEX IF NOT EXISTS idx_kategori_ust
    ON sistem.kategori (ust_kategori_id);

-- ---------------------------------------------------------------
-- 2) Baglanti kolonlari (legacy string kolonlar korunur)
-- ---------------------------------------------------------------
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS kategori_id BIGINT;
ALTER TABLE finans.masraf ADD COLUMN IF NOT EXISTS kategori_id BIGINT;
ALTER TABLE finans.butce ADD COLUMN IF NOT EXISTS kategori_id BIGINT;
ALTER TABLE ticaret.iskonto_kurali ADD COLUMN IF NOT EXISTS kategori_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_stok_kategori_id ON stok.stok (kategori_id);
CREATE INDEX IF NOT EXISTS idx_masraf_kategori_id ON finans.masraf (kategori_id);
CREATE INDEX IF NOT EXISTS idx_butce_kategori_id ON finans.butce (kategori_id);
CREATE INDEX IF NOT EXISTS idx_iskonto_kurali_kategori_id ON ticaret.iskonto_kurali (kategori_id);

-- ---------------------------------------------------------------
-- 3a) gelir_gider_kategori -> sistem.kategori (ID'LER KORUNUR)
--     GELIR -> KASA_GELIR, GIDER -> KASA_GIDER
-- ---------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('sistem.gelir_gider_kategori') IS NOT NULL THEN
        -- ID korunumu kritik: muhasebe.kasa_hareket.kategori_id eski ID'lere
        -- isaret ediyor (FK yok). ON CONFLICT DO NOTHING hem PK hem de
        -- (sirket_id, kapsam, lower(ad)) cakismasinda satiri atlar.
        INSERT INTO sistem.kategori (id, sirket_id, kapsam, ad, sira, aktif, olusturma_tarihi)
        SELECT g.id,
               g.sirket_id,
               CASE upper(COALESCE(g.tur, 'GIDER'))
                   WHEN 'GELIR' THEN 'KASA_GELIR'
                   ELSE 'KASA_GIDER'
               END,
               trim(g.ad),
               0,
               TRUE,
               COALESCE(g.olusturma_tarihi, NOW())
        FROM sistem.gelir_gider_kategori g
        WHERE g.ad IS NOT NULL AND trim(g.ad) <> ''
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- Sequence hizalanmasi: yeni kayitlar max(id)+1'den devam eder.
SELECT setval(
    pg_get_serial_sequence('sistem.kategori', 'id'),
    GREATEST(COALESCE((SELECT MAX(id) FROM sistem.kategori), 1), 1)
);

-- Eski tablo artik kullanilmiyor (entity kaldirildi).
DROP TABLE IF EXISTS sistem.gelir_gider_kategori;

-- ---------------------------------------------------------------
-- 3b) Legacy string kolonlardan distinct kategori adlari
--     MASRAF ve BUTCE adlari UPPER(TRIM(...)) ile normalize edilir.
--     Bu adlar uygulamada Java Locale.ROOT ile zaten buyuk harfe cevrilerek
--     yazildigi icin gercek veride upper() islevi etkisizdir; yalnizca eski
--     kucuk harfli satirlar normalize edilir.
-- ---------------------------------------------------------------
INSERT INTO sistem.kategori (sirket_id, kapsam, ad, sira, aktif)
SELECT DISTINCT s.sirket_id, 'STOK', trim(s.kategori), 0, TRUE
FROM stok.stok s
WHERE s.sirket_id IS NOT NULL
  AND s.kategori IS NOT NULL
  AND trim(s.kategori) <> ''
ON CONFLICT DO NOTHING;

INSERT INTO sistem.kategori (sirket_id, kapsam, ad, sira, aktif)
SELECT DISTINCT i.sirket_id, 'STOK', trim(i.kategori), 0, TRUE
FROM ticaret.iskonto_kurali i
WHERE i.sirket_id IS NOT NULL
  AND i.kategori IS NOT NULL
  AND trim(i.kategori) <> ''
ON CONFLICT DO NOTHING;

INSERT INTO sistem.kategori (sirket_id, kapsam, ad, sira, aktif)
SELECT DISTINCT m.sirket_id, 'MASRAF', upper(trim(m.kategori)), 0, TRUE
FROM finans.masraf m
WHERE m.sirket_id IS NOT NULL
  AND m.kategori IS NOT NULL
  AND trim(m.kategori) <> ''
ON CONFLICT DO NOTHING;

INSERT INTO sistem.kategori (sirket_id, kapsam, ad, sira, aktif)
SELECT DISTINCT b.sirket_id, 'BUTCE', upper(trim(b.kategori)), 0, TRUE
FROM finans.butce b
WHERE b.sirket_id IS NOT NULL
  AND b.kategori IS NOT NULL
  AND trim(b.kategori) <> ''
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------
-- 3c) kategori_id baglantilari (yalnizca bos olanlar)
-- ---------------------------------------------------------------
UPDATE stok.stok t
SET kategori_id = k.id
FROM sistem.kategori k
WHERE k.sirket_id = t.sirket_id
  AND k.kapsam = 'STOK'
  AND lower(k.ad) = lower(trim(t.kategori))
  AND t.kategori IS NOT NULL
  AND trim(t.kategori) <> ''
  AND t.kategori_id IS NULL;

UPDATE ticaret.iskonto_kurali t
SET kategori_id = k.id
FROM sistem.kategori k
WHERE k.sirket_id = t.sirket_id
  AND k.kapsam = 'STOK'
  AND lower(k.ad) = lower(trim(t.kategori))
  AND t.kategori IS NOT NULL
  AND trim(t.kategori) <> ''
  AND t.kategori_id IS NULL;

UPDATE finans.masraf t
SET kategori_id = k.id
FROM sistem.kategori k
WHERE k.sirket_id = t.sirket_id
  AND k.kapsam = 'MASRAF'
  AND lower(k.ad) = lower(trim(t.kategori))
  AND t.kategori IS NOT NULL
  AND trim(t.kategori) <> ''
  AND t.kategori_id IS NULL;

UPDATE finans.butce t
SET kategori_id = k.id
FROM sistem.kategori k
WHERE k.sirket_id = t.sirket_id
  AND k.kapsam = 'BUTCE'
  AND lower(k.ad) = lower(trim(t.kategori))
  AND t.kategori IS NOT NULL
  AND trim(t.kategori) <> ''
  AND t.kategori_id IS NULL;

-- ---------------------------------------------------------------
-- 3d) Legacy kolon normalizasyonu (MASRAF/BUTCE => UPPER(TRIM))
-- ---------------------------------------------------------------
UPDATE finans.masraf
SET kategori = upper(trim(kategori))
WHERE kategori IS NOT NULL AND kategori <> upper(trim(kategori));

UPDATE finans.butce
SET kategori = upper(trim(kategori))
WHERE kategori IS NOT NULL AND kategori <> upper(trim(kategori));
