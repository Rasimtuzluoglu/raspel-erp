-- =====================================================================
-- V165: Kitaplik (yer imleri + snippet'ler)
--
-- KULLANICI TALEBI:
--   "Yer imleri ve snippet eklenebilir" - tek modul, iki sekme.
--
-- TASARIM KARARLARI:
--   1) tur: YER_IMI (url) | SNIPPET (icerik).
--   2) Kapsam modeli sifre kasasi ile AYNI:
--        KISISEL -> yalniz kaydi tutan kullanici yazar ve gorur.
--        GLOBAL  -> yalniz ADMIN yazar.
--   3) gorunurluk (yalniz GLOBAL icin anlamli):
--        SAHIS -> yalniz ADMIN gorur (yonetici ozel notu).
--        TUMU  -> sirketteki herkes gorur (paylasimli kitaplik).
--      KISISEL kayitlarda gorunurluk daima SAHIS'tir.
--   4) Arsivleme (soft delete): fiziksel silme YOK; geri alinabilir.
--
-- NOT: Bu modul SIR ICERMEZ. Sifre kasasindan farkli olarak sifreleme,
-- no-store ve "acma denetimi" GEREKMEZ.
--
-- Idempotent.
-- =====================================================================

CREATE TABLE IF NOT EXISTS sistem.kitaplik (
    id                BIGSERIAL PRIMARY KEY,
    sirket_id         BIGINT       NOT NULL,
    -- KISISEL kayitlarda dolu, GLOBAL kayitlarda NULL.
    kullanici_id      BIGINT,
    -- YER_IMI | SNIPPET
    tur               VARCHAR(10)  NOT NULL,
    kapsam            VARCHAR(10)  NOT NULL DEFAULT 'KISISEL',
    gorunurluk        VARCHAR(10)  NOT NULL DEFAULT 'SAHIS',
    baslik            VARCHAR(200) NOT NULL,
    url               VARCHAR(1000),
    icerik            TEXT,
    -- SISTEM | ARAC | DOKUMAN | KOD | DIGER
    kategori          VARCHAR(40),
    notlar            TEXT,
    aktif             BOOLEAN      NOT NULL DEFAULT true,
    arsiv_tarihi      TIMESTAMP,
    olusturma_tarihi  TIMESTAMP    NOT NULL DEFAULT NOW(),
    guncelleme_tarihi TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kitaplik_sirket_aktif_tur_kapsam_baslik
    ON sistem.kitaplik (sirket_id, aktif, tur, kapsam, baslik);

CREATE INDEX IF NOT EXISTS idx_kitaplik_sirket_kullanici
    ON sistem.kitaplik (sirket_id, kullanici_id);

DO $$
DECLARE
    v_tablo  bigint;
    v_indeks bigint;
BEGIN
    SELECT count(*) INTO v_tablo FROM information_schema.tables
    WHERE table_schema = 'sistem' AND table_name = 'kitaplik';
    IF v_tablo = 0 THEN
        RAISE EXCEPTION 'V165 HATA: sistem.kitaplik tablosu olusturulamadi';
    END IF;

    SELECT count(*) INTO v_indeks FROM pg_indexes
    WHERE schemaname = 'sistem' AND indexname LIKE 'idx_kitaplik%';
    IF v_indeks < 2 THEN
        RAISE EXCEPTION 'V165 HATA: kitaplik indeksleri olusturulamadi (beklenen 2, bulunan %)', v_indeks;
    END IF;

    RAISE NOTICE 'V165: sistem.kitaplik olusturuldu, % indeks hazir', v_indeks;
END $$;