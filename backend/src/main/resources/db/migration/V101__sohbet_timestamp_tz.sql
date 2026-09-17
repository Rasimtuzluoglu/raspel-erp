-- V101__sohbet_timestamp_tz.sql
-- Sohbet zaman damgalarini timezone-aware (TIMESTAMPTZ) hale getirir.
-- Mevcut degerler UTC konteynerde (Docker) uretildiginden UTC kabul edilerek cevrilir.
ALTER TABLE sistem.sohbet_mesaj
    ALTER COLUMN olusturma_tarihi TYPE TIMESTAMPTZ USING olusturma_tarihi AT TIME ZONE 'UTC';

ALTER TABLE sistem.sohbet_oda
    ALTER COLUMN olusturma_tarihi TYPE TIMESTAMPTZ USING olusturma_tarihi AT TIME ZONE 'UTC';

ALTER TABLE sistem.sohbet_oda_uye
    ALTER COLUMN son_okuma TYPE TIMESTAMPTZ USING son_okuma AT TIME ZONE 'UTC';

ALTER TABLE sistem.sohbet_oda_uye
    ALTER COLUMN eklenme_tarihi TYPE TIMESTAMPTZ USING eklenme_tarihi AT TIME ZONE 'UTC';
