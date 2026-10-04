-- =====================================================================
-- V149: Yetki ve rol seed'i
--
-- DENETIM BULGUSU (C8 - CRITICAL):
--   Migration dosyalarinda INSERT INTO sistem.yetki / sistem.rol ifadesi YOK;
--   V145 ise bos bir tabloya CROSS JOIN yaziyordu (0 satir). Bu tablolar yalnizca
--   'Yetki Yonetimi' ekrani ilk kez ACILDIIGINDA dolduruluyordu.
--
--   Sonuc: YetkiKontrol.kontrol (fail-closed) yeni kurulumda rol satiri
--   bulamadigi icin USER rolunun @yetkiKontrol korumali TUM yazma uclari 403
--   donuyordu. Yonetim izin ekranini acana kadar sistem kullanilamaz durumdaydi.
--
-- COZUM:
--   1) Bu migration: yetki kodlari + roller + rol_yetki iliskileri seed edilir.
--   2) YetkiService.seedKontrolu() (YetkiSeedRunner): migration uygulanmamis
--      veya yarim kalmis veritabanlarinda ikinci guvenlik agi olarak devrede.
--
-- KAPSAM:
--   Yalnizca kodda GERCEKTEN kullanilan roller seed edilir: ADMIN, USER, SAHA,
--   DRIVER. PreAuthorize/hasRole kontrollerinde baska rol adi gecmez;
--   kullanilmayan rol eklemek yaniltici olur.
--
-- YETKI KURAMI:
--   ADMIN  = tum yetkiler.
--   USER   = operasyonel kapsam (tum modullerin READ/WRITE'i). DELETE ve
--           EXPORT kasitli olarak verilmez; bu yazma uclari yalnizca
--           Yetki Yonetimi ekranindan atanir.
--   SAHA   = saha personeli: stok/siparis/fatura/cari/ik okuma.
--   DRIVER = sofor: siparis/cari/stok okuma.
--
-- Idempotent: ON CONFLICT DO NOTHING ile tekrar calistirilabilir.
-- =====================================================================

-- 1) Yetki kodlari (38 kod)

INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('CARI_DELETE','Cari','Cari kart silme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('CARI_EXPORT','Cari','Cari listesi Excel/PDF aktar─▒m─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('CARI_READ','Cari','Cari hesaplar─▒ g├Âr├╝nt├╝leme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('CARI_WRITE','Cari','Cari kart ekleme ve d├╝zenleme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('FATURA_DELETE','Fatura','Fatura silme ve iptal etme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('FATURA_EXPORT','Fatura','Fatura listesi Excel/PDF aktar─▒m─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('FATURA_READ','Fatura','Faturalar─▒ g├Âr├╝nt├╝leme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('FATURA_WRITE','Fatura','Fatura olu┼şturma ve d├╝zenleme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('FINANS_DELETE','Finans','Finansal hareket silme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('FINANS_EXPORT','Finans','Ekstre ve finansal rapor aktar─▒m─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('FINANS_READ','Finans','Banka, kasa ve ├ğek/senet hareketlerini g├Âr├╝nt├╝leme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('FINANS_WRITE','Finans','Kasa/Banka i┼şlemi ve tahsilat/├Âdeme kayd─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('IK_DELETE','IK','Personel/izin kayd─▒ silme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('IK_EXPORT','IK','Bordro ve personel listesi aktar─▒m─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('IK_READ','IK','Personel, izin ve puantaj g├Âr├╝nt├╝leme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('IK_WRITE','IK','Personel kayd─▒, izin ve masraf onay─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('IRSALIYE_DELETE','Irsaliye','─░rsaliye kayd─▒ silme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('IRSALIYE_EXPORT','Irsaliye','─░rsaliye listesi aktar─▒m─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('IRSALIYE_READ','Irsaliye','Sevk ve gelen irsaliyeleri g├Âr├╝nt├╝leme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('IRSALIYE_WRITE','Irsaliye','─░rsaliye d├╝zenleme ve kabul') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('RAPOR_EXPORT','Rapor','Rapor ├ğ─▒kt─▒lar─▒n─▒ d─▒┼şa aktarma') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('RAPOR_READ','Rapor','Finansal ve ticari raporlar─▒ g├Âr├╝nt├╝leme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SATINALMA_DELETE','Satinalma','Sat─▒nalma kayd─▒ silme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SATINALMA_EXPORT','Satinalma','Sat─▒nalma raporlar─▒ aktar─▒m─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SATINALMA_READ','Satinalma','Sat─▒nalma talep ve sipari┼şlerini g├Âr├╝nt├╝leme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SATINALMA_WRITE','Satinalma','Sat─▒nalma talebi a├ğma ve sipari┼ş verme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SIPARIS_DELETE','Siparis','Sipari┼ş/teklif silme ve iptal') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SIPARIS_EXPORT','Siparis','Sipari┼ş ve teklif mektubu aktar─▒m─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SIPARIS_READ','Siparis','Sipari┼ş ve sat─▒┼ş tekliflerini g├Âr├╝nt├╝leme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SIPARIS_WRITE','Siparis','Yeni sipari┼ş ve teklif olu┼şturma') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SISTEM_DELETE','Sistem','Kullan─▒c─▒ ve sistem kayd─▒ silme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SISTEM_EXPORT','Sistem','Sistem denetim ve yedekleme aktar─▒m─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SISTEM_READ','Sistem','Sistem ayarlar─▒ ve kullan─▒c─▒lar─▒ g├Âr├╝nt├╝leme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('SISTEM_WRITE','Sistem','Kullan─▒c─▒, rol ve sistem ayarlar─▒ y├Ânetimi') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('STOK_DELETE','Stok','Stok kayd─▒ silme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('STOK_EXPORT','Stok','Stok listesi Excel/PDF aktar─▒m─▒') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('STOK_READ','Stok','Stok listesini g├Âr├╝nt├╝leme') ON CONFLICT (kod) DO NOTHING;
INSERT INTO sistem.yetki (kod, modul, aciklama) VALUES ('STOK_WRITE','Stok','Yeni stok ekleme ve g├╝ncelleme') ON CONFLICT (kod) DO NOTHING;


-- 2) Roller

INSERT INTO sistem.rol (ad, aciklama) VALUES ('ADMIN','Tam Yetkili Sistem Y├Âneticisi') ON CONFLICT (ad) DO NOTHING;
INSERT INTO sistem.rol (ad, aciklama) VALUES ('DRIVER','Sofor / Teslimat Kullanicisi') ON CONFLICT (ad) DO NOTHING;
INSERT INTO sistem.rol (ad, aciklama) VALUES ('SAHA','Saha Portal─▒ Kullanicisi') ON CONFLICT (ad) DO NOTHING;
INSERT INTO sistem.rol (ad, aciklama) VALUES ('USER','Standart Kullan─▒c─▒') ON CONFLICT (ad) DO NOTHING;


-- 3) Rol -> yetki iliskileri
--    Rol adina gore yetki kodu uzerinden baglanir; boylece yetki satir id
--    sirasi degisse bile iliski dogru kurulur.
DO $$
DECLARE
    v_admin_id  bigint;
    v_user_id   bigint;
    v_saha_id   bigint;
    v_driver_id bigint;
BEGIN
    SELECT id INTO v_admin_id  FROM sistem.rol WHERE ad = 'ADMIN';
    SELECT id INTO v_user_id   FROM sistem.rol WHERE ad = 'USER';
    SELECT id INTO v_saha_id   FROM sistem.rol WHERE ad = 'SAHA';
    SELECT id INTO v_driver_id FROM sistem.rol WHERE ad = 'DRIVER';

    -- ADMIN: tum yetkiler
    INSERT INTO sistem.rol_yetki (rol_id, yetki_id)
    SELECT v_admin_id, y.id FROM sistem.yetki y
    ON CONFLICT DO NOTHING;

    -- USER: tum modullerin READ/WRITE'i (DELETE ve EXPORT haric)
    INSERT INTO sistem.rol_yetki (rol_id, yetki_id)
    SELECT v_user_id, y.id FROM sistem.yetki y
    WHERE y.kod LIKE '%\_READ' OR y.kod LIKE '%\_WRITE'
    ON CONFLICT DO NOTHING;

    -- SAHA: saha personeli okuma kapsami
    INSERT INTO sistem.rol_yetki (rol_id, yetki_id)
    SELECT v_saha_id, y.id FROM sistem.yetki y
    WHERE y.kod IN ('STOK_READ','SIPARIS_READ','FATURA_READ','CARI_READ','IK_READ')
    ON CONFLICT DO NOTHING;

    -- DRIVER: sofor / teslimat okuma kapsami
    INSERT INTO sistem.rol_yetki (rol_id, yetki_id)
    SELECT v_driver_id, y.id FROM sistem.yetki y
    WHERE y.kod IN ('SIPARIS_READ','CARI_READ','STOK_READ')
    ON CONFLICT DO NOTHING;
END $$;

