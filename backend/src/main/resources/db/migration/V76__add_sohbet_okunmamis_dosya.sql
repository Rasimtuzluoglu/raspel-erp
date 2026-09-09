-- V76__add_sohbet_okunmamis_dosya.sql
-- Sohbet: okunmamis mesaj takibi (son_okuma) ve dosya/gorsel paylasimi (dosya_url).
ALTER TABLE sistem.sohbet_oda_uye ADD COLUMN IF NOT EXISTS son_okuma TIMESTAMP;
ALTER TABLE sistem.sohbet_mesaj ADD COLUMN IF NOT EXISTS dosya_url TEXT;
