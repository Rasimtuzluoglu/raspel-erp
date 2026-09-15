-- V88__stok_kodu_barkod_unique.sql
-- Sorun: stok.stok.stok_kodu ve barkod üzerinde benzersizlik kısıtı yoktu.
-- Tekilleştirme yalnızca application katmanında varsayılıyordu:
--   - findBySirketIdAndStokKodu -> Optional (mükerrer stok_kodu'nda
--     IncorrectResultSizeDataAccessException -> 500),
--   - findBySirketIdAndBarkod(...).stream().findFirst() -> mükerrer barkod'da
--     POS/hızlı satış (barkodIleBul) ve sayım (StokSayim.tara) SESSIZCE yanlış
--     stoğu seçebiliyordu.
-- Cozum: Tenant (sirket_id) bazli unique kısıtlar + stok_kodu NOT NULL (istege
-- bagli barkod nullable kalir ve ayni sirkette yalnizca bir kez kullanilabilir).
--   - Once mevcut mukererler ayiklanir (ayni anahtarda en dusuk id korunur).
--   - Bos/NULL stok_kodu kayitlari 'STK-<id>' ile doldurulur.
-- Not: Mukerer silme islemi harici FK (fatura_kalem vb.) referansina takilirsa
-- migration bilincli olarak basarisiz olur; temizlik operasyonla yapilmalidir.

-- 1) Bosluklari duzenle (barkod bos -> NULL)
UPDATE stok.stok SET stok_kodu = btrim(stok_kodu)
  WHERE stok_kodu IS NOT NULL AND stok_kodu <> btrim(stok_kodu);
UPDATE stok.stok SET barkod = NULL
  WHERE barkod IS NOT NULL AND btrim(barkod) = '';
UPDATE stok.stok SET barkod = btrim(barkod)
  WHERE barkod IS NOT NULL AND barkod <> btrim(barkod);

-- 2) Mukerer stok_kodu (tenant bazli, en dusuk id korunur)
DELETE FROM stok.stok
 WHERE stok_kodu IS NOT NULL
   AND id NOT IN (SELECT MIN(id) FROM stok.stok
                   WHERE stok_kodu IS NOT NULL
                   GROUP BY sirket_id, stok_kodu);

-- 3) Mukerer barkod (tenant bazli, en dusuk id korunur)
DELETE FROM stok.stok
 WHERE barkod IS NOT NULL
   AND id NOT IN (SELECT MIN(id) FROM stok.stok
                   WHERE barkod IS NOT NULL
                   GROUP BY sirket_id, barkod);

-- 4) Bos stok_kodu'na otomatik kod uret (stok_kodu artik zorunlu)
UPDATE stok.stok SET stok_kodu = 'STK-' || id
 WHERE stok_kodu IS NULL OR btrim(stok_kodu) = '';

-- 5) stok_kodu zorunlu yap
ALTER TABLE stok.stok ALTER COLUMN stok_kodu SET NOT NULL;

-- 6) Tenant bazli unique kisitlar
ALTER TABLE stok.stok ADD CONSTRAINT uk_stok_sirket_kod UNIQUE (sirket_id, stok_kodu);
ALTER TABLE stok.stok ADD CONSTRAINT uk_stok_sirket_barkod UNIQUE (sirket_id, barkod);

-- 7) Eski non-unique barkod index'i, unique kisitin index'iyle cakisti; kaldir
DROP INDEX IF EXISTS stok.idx_stok_barkod;