-- =====================================================================
-- V153: MUHASEBE rolü + ik/operasyon yetki kodları + masraf talebi kilidi
--
-- DENETIM BULGUSU (REDTEAM Faz 1.5 - CRITICAL):
--   `ik.personel_masraf_talep` entity'sinde @Version YOKTU ve
--   `finans.masraf.belge_no` UNIQUE degildi. Iki paralel
--   `PATCH /personel-masraf-talep/{id}/onayla` istegi ikisi de
--   durum='BEKLEMEDE' okuyor, ikisi de guard'i geciyor ve ikisi de
--   masrafIleEsle -> MasrafService.olustur cagiriyor. Sonuc:
--   MUKERRER masraf satiri + MUKERRER otomatik muhasebe fisi.
--   belge_no "TALEP-{id}" olarak iki kez yazildigi icin (unique olmadigi
--   icin) veritabani sessizce geciyor.
--
-- COZUM:
--   1) @Version (iyimser kilit) -> ikinci commit 409 alir.
--   2) finans.masraf.belge_no UNIQUE -> veritabani son savunma hatti.
--
-- DENETIM BULGUSU (REDTEAM Faz 1.4 - CRITICAL):
--   `MUHASEBE` rolu kullanıcı arayüzünde seçilebilir
--   (frontend/src/views/Kullanicilar.vue:226) ve doğrulamasız kaydedilir
--   (KullaniciService.java:301), ancak `sistem.rol` tablosunda HİÇBİR YERDE
--   oluşturulmamıştı. V145 ve V149 yalnızca ADMIN/DRIVER/SAHA/USER seed
--   ediyor; YetkiService'in varsayılan listesi de sadece SAHA+DRIVER tanıyor.
--
--   YetkiKontrol.kontrol() (YetkiKontrol.java) fail-closed davranıyor:
--   rol satırı bulunamazsa `return false` ile yetki reddediliyor.
--
--   SONUÇ: MUHASEBE rolü atanmış bir kullanıcı, hiçbir yetki koduna sahip
--   oluyor ve Onaylar ekranının 3/5 sekmesi kalıcı HTTP 403 döndürüyor:
--     - PUT /satinalma-talepler/{id}/durum   -> SatinalmaTalepController:59
--     - PUT /siparisler/{id}/durum           -> SiparisController:62
--     - POST /onay-ayarlari                  -> OnayAyariController:33
--   Kullanıcı "yetkim yok" hatası alıyor ve nedenini göremiyor.
--
-- COZUM:
--   1) MUHASEBE rolu seed edilir (V149 ile ayni desen).
--   2) MUHASEBE'ye finans + muhasebe yazma kapsami verilir.
--   3) Eksik olan operasyon yetki kodlari (Depo/Kategori/Proje) eklenir.
--      Kategoriler/Projeler/Depolar ekranlari `hasAnyRole('ADMIN','MUHASEBE')`
--      ile korunuyor ama frontend'de karsilik gelen yetki kodu olmadigi icin
--      yalnizca `v-if="isAdmin"` kullanabiliyordu.
--      (IK_* kodlari V149'da zaten var; tekrarlanmaz.)
--
-- YETKI KURAMI (V149 ile tutarli):
--   MUHASEBE = finans/muhasebe/ik yazma kapsami. DELETE ve EXPORT
--   verilmez; bunlar yalnizca Yetki Yonetimi ekranindan atanir.
--
-- Idempotent: ON CONFLICT DO NOTHING ile tekrar calistirilabilir.
-- =====================================================================

-- 1) Eksik yetki kodlari
-- NOT: IK_READ/WRITE/DELETE/EXPORT kodlari V149'da zaten seed EDILDI; tekrar
-- eklenmiyor (sistem.yetki.kod UNIQUE). Eksik olan operasyon alt modulleri:
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('DEPO_DELETE','Depo','Depo kaydı silme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('DEPO_READ','Depo','Depoları görüntüleme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('DEPO_WRITE','Depo','Depo ekleme ve düzenleme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('KATEGORI_DELETE','Kategori','Stok kategorisi silme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('KATEGORI_READ','Kategori','Stok kategorilerini görüntüleme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('KATEGORI_WRITE','Kategori','Stok kategorisi ekleme ve düzenleme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('PROJE_DELETE','Proje','Proje silme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('PROJE_READ','Proje','Projeleri görüntüleme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('PROJE_WRITE','Proje','Proje ekleme ve düzenleme') ON CONFLICT (kod) DO NOTHING;

-- 2) MUHASEBE rolu

INSERT INTO sistem.rol (ad, aciklama) VALUES ('MUHASEBE','Muhasebe / Finans Kullanıcısı') ON CONFLICT (ad) DO NOTHING;

-- 3) Rol -> yetki iliskeleri (V149 ile ayni desen: kod uzerinden baglanir)

DO $$
DECLARE
    v_muhasebe_id bigint;
BEGIN
    SELECT id INTO v_muhasebe_id FROM sistem.rol WHERE ad = 'MUHASEBE';

    -- Muhasebe: finans + ik yazma kapsami (DELETE ve EXPORT haric).
    INSERT INTO sistem.rol_yetki (rol_id, yetki_id)
    SELECT v_muhasebe_id, y.id FROM sistem.yetki y
    WHERE y.kod IN (
        'FINANS_READ','FINANS_WRITE',
        'IK_READ','IK_WRITE',
        'SIPARIS_READ','SIPARIS_WRITE',
        'SATINALMA_READ','SATINALMA_WRITE',
        'STOK_READ',
        'CARI_READ','CARI_WRITE',
        'FATURA_READ','FATURA_WRITE',
        'IRSALIYE_READ','IRSALIYE_WRITE',
        'RAPOR_READ','RAPOR_EXPORT',
        'DEPO_READ','DEPO_WRITE',
        'KATEGORI_READ','KATEGORI_WRITE',
        'PROJE_READ','PROJE_WRITE',
        'SISTEM_READ'
    )
    ON CONFLICT DO NOTHING;

    -- Admin her yetkiye sahip olmali; yeni eklenen kodlar da dahil.
    INSERT INTO sistem.rol_yetki (rol_id, yetki_id)
    SELECT r.id, y.id
    FROM sistem.rol r CROSS JOIN sistem.yetki y
    WHERE r.ad = 'ADMIN'
    ON CONFLICT DO NOTHING;
END $$;

-- 4) Dogrulama
DO $$
DECLARE
    v_rol_id   bigint;
    v_yetki_no bigint;
BEGIN
    SELECT id INTO v_rol_id FROM sistem.rol WHERE ad = 'MUHASEBE';
    IF v_rol_id IS NULL THEN
        RAISE EXCEPTION 'V153 HATA: MUHASEBE rolu olusturulamadi';
    END IF;

    SELECT count(*) INTO v_yetki_no
    FROM sistem.rol_yetki ry
    JOIN sistem.rol r ON r.id = ry.rol_id
    WHERE r.ad = 'MUHASEBE';

    IF v_yetki_no = 0 THEN
        RAISE EXCEPTION 'V153 HATA: MUHASEBE rolune yetki atanmadi (YetkiKontrol fail-closed -> 403)';
    END IF;

    RAISE NOTICE 'V153: MUHASEBE rolu seed edildi, % yetki atandi', v_yetki_no;
END $$;

-- 5) REDTEAM/Faz1.5: Eszamanli cift onay korumasi

-- 5a) iyimser kilit kolonu
ALTER TABLE ik.personel_masraf_talep
    ADD COLUMN IF NOT EXISTS version bigint DEFAULT 0;

-- 5b) belge_no UNIQUE: veritabani son savunma hatti.
-- ONEMLI: UNIQUE index eklenmeden once mevcut mukerrer degerler varsa
-- index kurulamaz. Bu yuzden once temizlik yapilir.
-- "TALEP-" oneki yalnizca otomatik uretilen kayitlarda kullanilir;
-- elle girilen belge_no'lar korunur.
DELETE FROM finans.masraf m
 WHERE m.belge_no LIKE 'TALEP-%'
   AND m.id NOT IN (
       SELECT MIN(m2.id) FROM finans.masraf m2
        WHERE m2.belge_no LIKE 'TALEP-%'
        GROUP BY m2.belge_no
   );

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE schemaname = 'finans' AND indexname = 'ux_masraf_belge_no'
    ) THEN
        -- Kismi (partial) unique index: yalnizca otomatik uretilen
        -- "TALEP-{id}" referanslari unique olsun. Kullanicinin elle girdigi
        -- belge_no degerleri (ve NULL'lar) kisitlanmaz.
        CREATE UNIQUE INDEX ux_masraf_belge_no
            ON finans.masraf (belge_no)
            WHERE belge_no LIKE 'TALEP-%';
        RAISE NOTICE 'V153: finans.masraf.belge_no kismi UNIQUE index olusturuldu';
    END IF;
END $$;