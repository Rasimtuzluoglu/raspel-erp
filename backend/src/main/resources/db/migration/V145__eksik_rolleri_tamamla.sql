-- V145__eksik_rolleri_tamamla.sql
-- Kullanici tablosunda kullanilan ancak sistem.rol tablosunda satiri bulunmayan
-- roller (DRIVER, SAHA) olusturulur. YetkiKontrol, kullanici rolu sistemde
-- tanimli degilse sorguyu null dondurur ve her istegi reddeder; bu roller
-- olmadan saha/sofor akislari yetki kontrolunden gecemez.
--
-- Yetkiler en az yetki ilkesiyle verilir (yalnizca okuma). Yazma yetkileri
-- yalnizca YetkiYonetimi ekranindan atanir.
INSERT INTO sistem.rol (ad, aciklama)
VALUES ('DRIVER', 'Sofor / Teslimat Kullanicisi')
ON CONFLICT (ad) DO NOTHING;

INSERT INTO sistem.rol (ad, aciklama)
VALUES ('SAHA', 'Saha Portalı Kullanicisi')
ON CONFLICT (ad) DO NOTHING;

-- Sofor: siparis ve cari okuma, stok goruntuleme.
INSERT INTO sistem.rol_yetki (rol_id, yetki_id)
SELECT r.id, y.id
FROM sistem.rol r
CROSS JOIN sistem.yetki y
WHERE r.ad = 'DRIVER'
  AND y.kod IN ('SIPARIS_READ', 'CARI_READ', 'STOK_READ')
ON CONFLICT DO NOTHING;

-- Saha personeli: stok, siparis, fatura, cari ve personel okuma.
INSERT INTO sistem.rol_yetki (rol_id, yetki_id)
SELECT r.id, y.id
FROM sistem.rol r
CROSS JOIN sistem.yetki y
WHERE r.ad = 'SAHA'
  AND y.kod IN ('STOK_READ', 'SIPARIS_READ', 'FATURA_READ', 'CARI_READ', 'IK_READ')
ON CONFLICT DO NOTHING;
