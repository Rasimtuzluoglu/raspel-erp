-- Faz 2: Stok gruplama alanlarinin normalizasyonu ve indekslenmesi.
--
-- BAĞLAM: `stok.stok` üzerinde iki AYRI sınıflandırma kolonu vardır ve veri
-- incelemesi ikisinin farklı kavramları taşıdığını gösterdi:
--   * kategori    -> ürün cinsi      (Gıda / Ambalaj / Yapı)
--   * stok_grubu  -> üretim malzeme  (Hammadde / Mamul / Sarf / Aksesuar)
-- Bu migration alanları BİRLEŞTİRMEZ; hiçbir üretim tipi bilgisi kaybolmaz.
--
-- Sorun: ikisi de serbest metin (`VARCHAR(100)`), normalizasyon ve indeks yok.
-- "Gıda", "gıda", " Gıda " üç ayrı grup olarak birikir; filtreler tam `=`
-- ve büyük/küçük harf duyarlı olduğu için sessizce boş sonuç döner.

-- 1) Normalizasyon: kenar boşlukları at, içerideki tekrar eden boşlukları
--    teke indir, boş kalanları NULL yap (NULL ve '' tek "boş" anlamına gelir).
UPDATE stok.stok
SET kategori = NULLIF(btrim(regexp_replace(coalesce(kategori, ''), '\s+', ' ', 'g')), '')
WHERE kategori IS NOT NULL;

UPDATE stok.stok
SET stok_grubu = NULLIF(btrim(regexp_replace(coalesce(stok_grubu, ''), '\s+', ' ', 'g')), '')
WHERE stok_grubu IS NOT NULL;

-- 2) Sınıflandırma filtreleri her zaman tam `=` ile çalışır. Çoğu katalogda
--    aynı kavram farklı yazımla girer ("Gıda" / "gıda" / "GIDA"); indeks
--    hazır olsun ki normalize edilmiş değerler büyük/küçük harf duyarsız
--    karşılaştırılabilsin (bkz. StokRepository.filtreli).
CREATE INDEX IF NOT EXISTS idx_stok_kategori_lower
    ON stok.stok (lower(kategori));

CREATE INDEX IF NOT EXISTS idx_stok_stok_grubu_lower
    ON stok.stok (lower(stok_grubu));

-- 3) Satış ekranı yazarken ürün araması (StokRepository.satisOnerileri)
--    `LOWER(s.ad) LIKE 'q%'` önek eşleşmesi yapar. Fonksiyonel indeks olmadan
--    bu ifade indeks kullanmaz ve büyük kataloglarda her tuşta tam tarama yapar.
CREATE INDEX IF NOT EXISTS idx_stok_ad_lower
    ON stok.stok (lower(ad));

-- Aynı sebeple cari araması da indekslenir (bkz. CariHesapService.filtreli).
CREATE INDEX IF NOT EXISTS idx_cari_hesap_ad_lower
    ON cari.cari_hesap (lower(ad));
