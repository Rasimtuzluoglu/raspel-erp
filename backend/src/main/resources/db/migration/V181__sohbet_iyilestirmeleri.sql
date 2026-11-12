-- V181: sohbet iyilestirmeleri (Faz 8).
--
-- 1) Ozel kanal: yalnizca uye olanlar gorur ve yalnizca yonetici uye ekleyebilir.
-- 2) Genel sohbet sayfalamasi icin (sirket_id, oda_id, id) bilesik indeks;
--    mevcut indeks tarih bazliydi, cursor sayfalamasi id uzerinden gidiyor.
ALTER TABLE sistem.sohbet_oda ADD COLUMN IF NOT EXISTS ozel BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_sohbet_mesaj_genel
    ON sistem.sohbet_mesaj (sirket_id, oda_id, id);
