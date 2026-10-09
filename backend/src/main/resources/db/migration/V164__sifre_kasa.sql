-- =====================================================================
-- V164: Sifre kasasi
--
-- KULLANICI TALEBI:
--   "Sifre kasasi - sifreleri unutmamak icin not alabilecegim bir kisim
--    istiyorum. Her kullanici kendi sifrelerini yazabilecek, hem de
--    global olarak gorunebilecek sifreleri secebilecek."
--
-- TASARIM KARARLARI (onaylanmis):
--   1) Kirli metin YOK: sifre AES-256-GCM ile sifrelenir (util/AesGcmUtil)
--      ve yalniz Base64 metni saklanir. Anahtar ortam degiskeninden gelir
--      (APP_VAULT_ENCRYPTION_KEY); veritabani yedegi ele gecse bile
--      sifreler duz metin okunamaz.
--   2) Iki kapsam:
--        KISISEL -> yalniz kaydi tutan kullanici yazar ve gorur.
--        GLOBAL  -> yalniz ADMIN yazar; meta bilgileri herkese aciktir.
--   3) sifre_gorunurlugu (yalniz GLOBAL kapsamda anlamli):
--        SAHIS -> sifreyi yazan + ADMIN acar.
--        TUMU  -> herkes acar (ofis agi gibi ortak erisim senaryosu).
--      Her acma islemi sistem.audit_log'a SIFRE_GORUNTULENME olarak yazilir.
--   4) Arsivleme (soft delete): fiziksel silme YOK. Silinen kayitlar
--      aktif=false olur ve geri alinabilir. Yanlislikla arsivlenen bir
--      kayit kalici olarak kaybolmamalidir.
--   5) Gecerlilik: gecerlilik_gun doluysa sifre_degisim_tarihi'ne gore
--      sure durumu hesaplanir (SURESIZ/GECERLI/SURE_YAKLASTI/SURESI_BITTI).
--      Arsivlenen kayitlar sure uyarisina girmez.
--
-- IDOR/KIRILABILIRLIK KORUMASI:
--   Liste sorgulari her zaman sirket_id ile baslar; sorgu seviyesinde
--   tenant filtresi uygulanir (servis kontrolu atlanirsa bile sizinti olmaz).
--
-- Idempotent.
-- =====================================================================

CREATE TABLE IF NOT EXISTS sistem.sifre_kasa (
    id                    BIGSERIAL PRIMARY KEY,
    -- Tenant kolonu: diger sistem tablolari ile ayni tipte (NOT NULL).
    sirket_id             BIGINT       NOT NULL,
    -- KISISEL kayitlarda dolu, GLOBAL kayitlarda NULL.
    kullanici_id          BIGINT,
    -- KISISEL | GLOBAL
    kapsam                VARCHAR(10)  NOT NULL DEFAULT 'KISISEL',
    -- SAHIS | TUMU (yalniz GLOBAL kapsamda TUMU secilebilir)
    sifre_gorunurlugu     VARCHAR(10)  NOT NULL DEFAULT 'SAHIS',
    baslik                VARCHAR(200) NOT NULL,
    kullanici_adi         VARCHAR(200),
    -- AES-256-GCM sifreli sifre, Base64 (12 bayt IV + ciphertext + GCM tag).
    sifre_cipher          TEXT         NOT NULL,
    url                   VARCHAR(500),
    -- SISTEM | BANKA | EPOSTA | SOSYAL | DIGER
    kategori              VARCHAR(40),
    notlar                TEXT,
    -- NULL veya 0 -> sifresiz. Diger degerler gun sayisi.
    gecerlilik_gun        INTEGER,
    -- Sifre metni en son yazildigi an. Yalniz SIFRE alani degistiginde
    -- sifirlanir; baslik/not duzenlemesi surEYI uzatmaz.
    sifre_degisim_tarihi  TIMESTAMP,
    -- Sifre en son kime gosterildi.
    son_goruntuleme       TIMESTAMP,
    -- Soft delete. Arsivlenen kayitlar listeden dusulur, geri alinabilir.
    aktif                 BOOLEAN      NOT NULL DEFAULT true,
    arsiv_tarihi          TIMESTAMP,
    olusturma_tarihi      TIMESTAMP    NOT NULL DEFAULT NOW(),
    guncelleme_tarihi     TIMESTAMP
);

-- Liste ekraninin ana erisim yolu: sirket + aktif + kapsam filtresi, basliga gore.
CREATE INDEX IF NOT EXISTS idx_sifre_kasa_sirket_aktif_kapsam_baslik
    ON sistem.sifre_kasa (sirket_id, aktif, kapsam, baslik);

-- Kisisel kasaya erisim: "bu kullanicinin kisisel kayitlari".
CREATE INDEX IF NOT EXISTS idx_sifre_kasa_sirket_kullanici
    ON sistem.sifre_kasa (sirket_id, kullanici_id);

-- Dogrulama (fail-fast assertion): tablo ve indeksler gercekten olustu mu?
DO $$
DECLARE
    v_tablo_sayi   bigint;
    v_indeks_sayi  bigint;
BEGIN
    SELECT count(*) INTO v_tablo_sayi
    FROM information_schema.tables
    WHERE table_schema = 'sistem' AND table_name = 'sifre_kasa';

    IF v_tablo_sayi = 0 THEN
        RAISE EXCEPTION 'V164 HATA: sistem.sifre_kasa tablosu olusturulamadi';
    END IF;

    SELECT count(*) INTO v_indeks_sayi
    FROM pg_indexes
    WHERE schemaname = 'sistem' AND indexname LIKE 'idx_sifre_kasa%';

    IF v_indeks_sayi < 2 THEN
        RAISE EXCEPTION 'V164 HATA: sifre_kasa indeksleri olusturulamadi (beklenen 2, bulunan %)', v_indeks_sayi;
    END IF;

    RAISE NOTICE 'V164: sistem.sifre_kasa olusturuldu, % indeks hazir', v_indeks_sayi;
END $$;