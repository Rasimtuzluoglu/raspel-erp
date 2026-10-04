-- =====================================================================
-- RAZPEK RED TEAM - TEMIZLEME
-- Yalnizca ZZTEST_ onekli kayitlari ve id>=9900 kayitlarini siler.
-- Sirket 4 (RasPel Test) mevcut gercek verisi ASLA dokunulmaz.
-- Kullanim: psql -f 99_RAZPEK_TEMIZLEME.sql
-- =====================================================================

BEGIN;

-- 1) Kullanici kaynakli tum kayitlar (ON DELETE CASCADE zinciri ile)
--    Not: bazi tablolarda FK 'ON DELETE CASCADE' yok; oncesi bagli kayitlar silinir.
\echo '--- Hata kayitlari temizleniyor'
DELETE FROM sistem.audit_log      WHERE sirket_id IN (4, 99) AND (detay LIKE '%ZZTEST%' OR aciklama LIKE '%ZZTEST%');
DELETE FROM sistem.audit_log      WHERE kullanici_id >= 9900;

\echo '--- Bildirimler'
DELETE FROM sistem.bildirim       WHERE kullanici_id >= 9900 OR sirket_id = 99;
DELETE FROM sistem.ajanda_gorev   WHERE kullanici_id >= 9900;
DELETE FROM sistem.ajanda_hatirlatici WHERE kullanici_id >= 9900;
DELETE FROM sistem.api_token      WHERE kullanici_id >= 9900;
DELETE FROM sistem.sifre_sifirla_token WHERE kullanici_id >= 9900;
DELETE FROM sistem.sohbet_mesaj   WHERE oda_id IN (SELECT id FROM sistem.sohbet_oda WHERE olusturan_kullanici_id >= 9900);
DELETE FROM sistem.sohbet_oda_uye WHERE oda_id IN (SELECT id FROM sistem.sohbet_oda WHERE olusturan_kullanici_id >= 9900);
-- NOT (canli dogrulama): 'sistem.kullanici_rol' tablosu YOK; kullanici-rol
-- iliskisi ManyToMany olarak Kullanici entity'sinde tanimli ve FK'siz
-- (sirket_id uzerinden yonlendirilir), bu yuzden silme gerekmez.
DELETE FROM sistem.sohbet_oda     WHERE olusturan_kullanici_id >= 9900;
DELETE FROM sistem.notlar         WHERE kullanici_id >= 9900 OR sirket_id = 99;

\echo '--- Finansal kayitlar'
-- Iadeleronce silinmeli: fatura.fatura silinirken iade.fatura_id FK'si ihlal
-- ediyordu (fk_iade_fatura).
DELETE FROM muhasebe.irsaliye_kalem WHERE irsaliye_id IN (SELECT id FROM muhasebe.irsaliye WHERE irsaliye_no LIKE 'ZZ%');
DELETE FROM muhasebe.irsaliye       WHERE irsaliye_no LIKE 'ZZ%';
DELETE FROM ticaret.iade_kalem      WHERE iade_id IN (SELECT id FROM ticaret.iade WHERE fatura_id IN (
    SELECT id FROM fatura.fatura WHERE cari_hesap_id IN (SELECT id FROM cari.cari_hesap WHERE ad LIKE 'ZZTEST%')));
DELETE FROM ticaret.iade            WHERE fatura_id IN (
    SELECT id FROM fatura.fatura WHERE cari_hesap_id IN (SELECT id FROM cari.cari_hesap WHERE ad LIKE 'ZZTEST%'));
DELETE FROM muhasebe.kasa_hareket        WHERE kasa_id IN (SELECT id FROM muhasebe.kasa WHERE ad LIKE 'ZZTEST%');
DELETE FROM finans.banka_hareketi      WHERE banka_id IN (SELECT id FROM muhasebe.banka WHERE ad LIKE 'ZZTEST%');
DELETE FROM cari.hareket                 WHERE cari_hesap_id IN (SELECT id FROM cari.cari_hesap WHERE ad LIKE 'ZZTEST%');
DELETE FROM fatura.fatura_kalem          WHERE fatura_id IN (SELECT id FROM fatura.fatura WHERE cari_hesap_id IN (SELECT id FROM cari.cari_hesap WHERE ad LIKE 'ZZTEST%'));
DELETE FROM fatura.fatura                WHERE cari_hesap_id IN (SELECT id FROM cari.cari_hesap WHERE ad LIKE 'ZZTEST%');
DELETE FROM muhasebe.muhasebe_fis_kalem  WHERE fis_id IN (
    SELECT m.id FROM muhasebe.muhasebe_fisi m
    JOIN fatura.fatura f ON f.id = m.kaynak_id
    JOIN cari.cari_hesap c ON c.id = f.cari_hesap_id
    WHERE c.ad LIKE 'ZZTEST%' AND m.kaynak_tip = 'FATURA');
DELETE FROM muhasebe.muhasebe_fisi       WHERE kaynak_tip = 'FATURA' AND kaynak_id IN (
    SELECT f.id FROM fatura.fatura f JOIN cari.cari_hesap c ON c.id = f.cari_hesap_id WHERE c.ad LIKE 'ZZTEST%');

\echo '--- Stok hareketleri'
DELETE FROM stok.stok_hareket   WHERE stok_id IN (SELECT id FROM stok.stok WHERE stok_kodu LIKE 'ZZTEST%');
DELETE FROM stok.stok_duzeltme  WHERE stok_id IN (SELECT id FROM stok.stok WHERE stok_kodu LIKE 'ZZTEST%');
DELETE FROM stok.stok_fiyat     WHERE stok_id IN (SELECT id FROM stok.stok WHERE stok_kodu LIKE 'ZZTEST%');
DELETE FROM stok.recete_kalem   WHERE recete_id IN (SELECT id FROM stok.recete WHERE ad LIKE 'ZZ%');
DELETE FROM stok.recete         WHERE ad LIKE 'ZZ%';
-- Uretim emirleri: 'recete_id' kolonu YOK; urun_id uzerinden baglanir.
DELETE FROM stok.uretim_emri_log WHERE uretim_emri_id IN (
    SELECT e.id FROM stok.uretim_emri e
    WHERE e.urun_id IN (SELECT id FROM stok.stok WHERE stok_kodu LIKE 'ZZTEST%'));
DELETE FROM stok.uretim_emri      WHERE urun_id IN (SELECT id FROM stok.stok WHERE stok_kodu LIKE 'ZZTEST%');

\echo '--- Siparis / teklif (irsaliye ve iade yukarida silindi)'
DELETE FROM siparis.siparis_kalem WHERE siparis_id IN (SELECT id FROM siparis.siparis WHERE siparis_no LIKE 'ZZ%');
DELETE FROM siparis.siparis       WHERE siparis_no LIKE 'ZZTEST%';
DELETE FROM ticaret.teklif_kalem  WHERE teklif_id IN (SELECT id FROM ticaret.teklif WHERE teklif_no LIKE 'ZZTEST%');
DELETE FROM ticaret.teklif        WHERE teklif_no LIKE 'ZZTEST%';

\echo '--- Personel / IK'
DELETE FROM ik.maas_bordro             WHERE personel_id IN (SELECT id FROM personel.personel WHERE ad LIKE 'ZZTEST%');
DELETE FROM ik.personel_masraf_talep   WHERE personel_id IN (SELECT id FROM personel.personel WHERE ad LIKE 'ZZTEST%');
DELETE FROM personel.personel_izin       WHERE personel_id IN (SELECT id FROM personel.personel WHERE ad LIKE 'ZZTEST%');
DELETE FROM personel.personel_puantaj   WHERE personel_id IN (SELECT id FROM personel.personel WHERE ad LIKE 'ZZTEST%');
DELETE FROM personel.personel           WHERE ad LIKE 'ZZTEST%';
DELETE FROM ik.vardiya                 WHERE personel_id IN (SELECT id FROM personel.personel WHERE ad LIKE 'ZZTEST%');

\echo '--- Temel kayitlar'
DELETE FROM cari.cari_fiyat  WHERE stok_id IN (SELECT id FROM stok.stok WHERE stok_kodu LIKE 'ZZTEST%');
DELETE FROM stok.stok_fiyat  WHERE stok_id IN (SELECT id FROM stok.stok WHERE stok_kodu LIKE 'ZZTEST%');
DELETE FROM cari.cari_hesap  WHERE ad LIKE 'ZZTEST%';
DELETE FROM stok.stok        WHERE stok_kodu LIKE 'ZZTEST%';
DELETE FROM muhasebe.kasa    WHERE ad LIKE 'ZZTEST%';
DELETE FROM muhasebe.banka   WHERE ad LIKE 'ZZTEST%';

\echo '--- Tenant B tamamen (sirket referansi olan TUM tablolar)'
-- NOT (canli dogrulama):
--  * information_schema.constraint_column_usage, PostgreSQL'de FK icin BOS doner;
--    bu yuzden pg_catalog (pg_constraint.confrelid) kullanildi.
--  * sistem.sirket'i 69 tablo referansliyor; hepsi sirket_id = 99 ile
--    temizlenir, aksi halde fk_*_sirket ihlaliyle rollback olur.
\i _tenant_b_temizle.sql

DELETE FROM sistem.kullanici_sirket WHERE sirket_id = 99 OR kullanici_id >= 9900;
DELETE FROM sistem.kullanici  WHERE id >= 9900;
DELETE FROM sistem.donem      WHERE sirket_id = 99;
DELETE FROM sistem.sirket     WHERE id = 99;

COMMIT;

\echo '=== DOGRULAMA (hepsi 0 olmali) ==='
SELECT 'sirket_99'  AS kontrol, count(*) FROM sistem.sirket     WHERE id = 99
UNION ALL SELECT 'kullanici_99xx', count(*) FROM sistem.kullanici WHERE id >= 9900
UNION ALL SELECT 'cari_zztest',    count(*) FROM cari.cari_hesap  WHERE ad LIKE 'ZZTEST%'
UNION ALL SELECT 'stok_zztest',    count(*) FROM stok.stok        WHERE stok_kodu LIKE 'ZZTEST%'
UNION ALL SELECT 'kasa_zztest',    count(*) FROM muhasebe.kasa    WHERE ad LIKE 'ZZTEST%'
UNION ALL SELECT 'banka_zztest',   count(*) FROM muhasebe.banka   WHERE ad LIKE 'ZZTEST%'
UNION ALL SELECT 'irsaliye_zz',    count(*) FROM muhasebe.irsaliye WHERE irsaliye_no LIKE 'ZZ%'
UNION ALL SELECT 'recete_zz',      count(*) FROM stok.recete      WHERE ad LIKE 'ZZ%'
UNION ALL SELECT 'iade_zz',        count(*) FROM ticaret.iade     WHERE aciklama LIKE 'ZZ%';

\echo '=== SIRKET 4 MEVCUT VERI (dokunulmamali) ==='
SELECT count(*) AS kalan_cari   FROM cari.cari_hesap WHERE sirket_id = 4;
SELECT count(*) AS kalan_stok   FROM stok.stok       WHERE sirket_id = 4;
SELECT count(*) AS kalan_kasa   FROM muhasebe.kasa   WHERE sirket_id = 4;
SELECT count(*) AS kalan_banka  FROM muhasebe.banka  WHERE sirket_id = 4;
-- ZZTEST cariye ait artik hareket/fatura KALMAMALI
SELECT count(*) AS kalan_zz_hareket FROM cari.hareket
  WHERE cari_hesap_id IN (SELECT id FROM cari.cari_hesap WHERE ad LIKE 'ZZTEST%');
SELECT count(*) AS kalan_zz_fatura FROM fatura.fatura
  WHERE cari_hesap_id IN (SELECT id FROM cari.cari_hesap WHERE ad LIKE 'ZZTEST%');
SELECT count(*) AS kalan_zz_stok_hareket FROM stok.stok_hareket
  WHERE stok_id IN (SELECT id FROM stok.stok WHERE stok_kodu LIKE 'ZZTEST%');