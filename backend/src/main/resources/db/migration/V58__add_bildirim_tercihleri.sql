-- V58__add_bildirim_tercihleri.sql
-- Kullanıcı bazlı bildirim tercihleri (JSON dizi: hangi bildirim tiplerinin istendiği).
ALTER TABLE sistem.kullanici ADD COLUMN IF NOT EXISTS bildirim_tercihleri TEXT;
