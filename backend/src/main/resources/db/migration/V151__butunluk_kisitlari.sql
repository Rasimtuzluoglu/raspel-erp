-- =====================================================================
-- V151: Veri bütünlüğü kısıtları
--
-- DENETIM BULGUSU (HIGH - veri bütünlüğü):
--   Uygulama katmani dogrulamalari tek basina yetmez: import (Excel),
--   toplu islem ve dogrudan API cagrilari dogrulama bypass edebiliyor.
--   Veritabani seviyesinde hicbir CHECK/UNIQUE kisiti yoktu; bozuk veri
--   yalnizca uygulama mantigi ile yakalaniyordu. Uygulama hicbir hata
--   vermezse veri sessizce bozuluyor ve raporlar tutarsiz hale geliyor.
--
--   Somut riskler:
--   - Ayni sirket icin AYNI DONEM iki kez acilabiliyor -> yil sonu
--     kapanisi iki kez calisir, muhasebe cift kayit.
--   - Negatif stok miktari yazilabiliyor -> raporlar yanlis, stok
--     sayimlari tutmaz.
--   - Negatif fatura tutarlari yazilabiliyor -> KDV beyannamesi hatali.
--   - Izin baslangic > bitis yazilabiliyor -> izin gun hesabi negatif.
--   - Ayni stok kodu iki kez tanimlanabiliyor -> barkod/stok eslesmesi
--     belirsiz, sayim tutmuyor.
--
-- ONCE VERI DOGRULANDI (canli DB'de SELECT ile):
--   negatif stok            : 0 satir
--   negatif fatura tutari   : 0 satir
--   bitis < baslangic       : 0 satir (donem, personel_izin)
--   ayni (sirket, stok_kodu): 0 grup
--   ayni (sirket, vkn)      : 0 grup
--   ayni (sirket, donem baslangic): 0 grup
--   => Bu migration HICBIR mevcut satiri degistirmez/silmez; yalnizca
--      gelecekteki yazmalari kısıtlar.
--
-- DIKKAT:
--   cari_hesap.bakiye icin CHECK EKLENMEDI. Negatif bakiye bu sistemde
--   GECERLI bir is kuralidir: negatif = cari bize borclu (tahsil
--   edilecek), pozitif = biz cariye borcluyuz. Kisit eklemek mevru
--   carileri reddederdi.
--
-- Idempotent: IF NOT EXISTS ile tekrar calistirilabilir.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Donem tekilligi: ayni sirket icin ayni baslangic tarihli iki donem
--    acilmasini engeller (yil sonu kapanis cift calismasi).
-- ---------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS ux_donem_sirket_baslangic
    ON sistem.donem (sirket_id, baslangic);

-- ---------------------------------------------------------------------
-- 2) Tarih araligi tutarliligi
-- ---------------------------------------------------------------------
ALTER TABLE sistem.donem DROP CONSTRAINT IF EXISTS ck_donem_tarih_sirasi;
ALTER TABLE sistem.donem ADD CONSTRAINT ck_donem_tarih_sirasi
    CHECK (bitis >= baslangic);

ALTER TABLE personel.personel_izin DROP CONSTRAINT IF EXISTS ck_personel_izin_tarih_sirasi;
ALTER TABLE personel.personel_izin ADD CONSTRAINT ck_personel_izin_tarih_sirasi
    CHECK (bitis IS NULL OR bitis >= baslangic);

-- ---------------------------------------------------------------------
-- 3) Stok miktari negatif olamaz.
--    NOT VALID KULLANILMAZ: mevcut veri dogrulandi (0 ihlal) ve kisa
--    tablolarda dogrulama anliktir. Tam dogrulama yapilirsa ileride
--    veri eklendiginde de kisit aninda uygulanir.
-- ---------------------------------------------------------------------
ALTER TABLE stok.stok DROP CONSTRAINT IF EXISTS ck_stok_miktar_pozitif;
ALTER TABLE stok.stok ADD CONSTRAINT ck_stok_miktar_pozitif
    CHECK (miktar IS NULL OR miktar >= 0);

ALTER TABLE stok.stok DROP CONSTRAINT IF EXISTS ck_stok_min_miktar_pozitif;
ALTER TABLE stok.stok ADD CONSTRAINT ck_stok_min_miktar_pozitif
    CHECK (min_miktar IS NULL OR min_miktar >= 0);

-- NOT: Stok kodu tekilligi (sirket_id, stok_kodu) ZATEN mevcut:
--   uk_stok_sirket_kod UNIQUE INDEX. Burada tekrar eklenmez; ayni
--   kapsama sahip ikinci bir index yazma maliyeti getirir, kazandirmaz.

-- ---------------------------------------------------------------------
-- 4) Fatura tutarlari negatif olamaz (KDV beyannamesi bütünlüğü).
-- ---------------------------------------------------------------------
ALTER TABLE fatura.fatura DROP CONSTRAINT IF EXISTS ck_fatura_ara_toplam_pozitif;
ALTER TABLE fatura.fatura ADD CONSTRAINT ck_fatura_ara_toplam_pozitif
    CHECK (ara_toplam IS NULL OR ara_toplam >= 0);

ALTER TABLE fatura.fatura DROP CONSTRAINT IF EXISTS ck_fatura_kdv_pozitif;
ALTER TABLE fatura.fatura ADD CONSTRAINT ck_fatura_kdv_pozitif
    CHECK (kdv IS NULL OR kdv >= 0);

ALTER TABLE fatura.fatura DROP CONSTRAINT IF EXISTS ck_fatura_genel_toplam_pozitif;
ALTER TABLE fatura.fatura ADD CONSTRAINT ck_fatura_genel_toplam_pozitif
    CHECK (genel_toplam IS NULL OR genel_toplam >= 0);

ALTER TABLE fatura.fatura DROP CONSTRAINT IF EXISTS ck_fatura_iskonto_pozitif;
ALTER TABLE fatura.fatura ADD CONSTRAINT ck_fatura_iskonto_pozitif
    CHECK (genel_iskonto_tutari IS NULL OR genel_iskonto_tutari >= 0);

-- ---------------------------------------------------------------------
-- 5) Cari vergi numarasi sirket ici tekildir.
--    NULL/'' degerler haric tutulur (bos VKN birden fazla kayitta
--    normaldir; benzersizlik yalnizca dolu VKN icin gecerli).
--
--    NOT: Zaten mevcut -> uk_cari_vergi_no_sirket UNIQUE INDEX.
--    Ayni kapsama sahip ikinci bir index yazma maliyeti getirir.
--    Burada tekrar eklenmez.
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- 6) Kasa bakiyesi tutarli kalsin: kasa hareketi tutari negatif OLABILIR
--    (gider), ancak cari/fatura tutarlari gibi. Bu yuzden burada
--    kisit eklenmez; yalnizca not olarak belirtilir.
-- ---------------------------------------------------------------------
-- (bkz. dosya basindaki DIKKAT: cari_hesap.bakiye negatif olabilir)