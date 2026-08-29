-- V61__add_kullanici_saha_kullanici.sql
-- Saha personelini ayirt etmek icin flag. Saha kullanicilari USER rolunde kalir
-- (backend erisimi icin) ancak saha_kullanici=true ile frontend'de kilitli portal gorurler.
ALTER TABLE sistem.kullanici ADD COLUMN IF NOT EXISTS saha_kullanici BOOLEAN NOT NULL DEFAULT FALSE;
