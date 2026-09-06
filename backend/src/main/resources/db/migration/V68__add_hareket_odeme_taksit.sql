-- V68__add_hareket_odeme_taksit.sql
-- Tahsilat/odeme hareketine odeme yontemi (NAKIT/KART/TAKSIT/HAVALE) ve taksit bilgisi eklenir.
ALTER TABLE cari.hareket ADD COLUMN IF NOT EXISTS odeme_yontemi VARCHAR(20);
ALTER TABLE cari.hareket ADD COLUMN IF NOT EXISTS taksit_kurum VARCHAR(255);
ALTER TABLE cari.hareket ADD COLUMN IF NOT EXISTS taksit_tutar NUMERIC(19, 2);
