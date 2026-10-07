-- V161__recete_baz_miktar.sql
-- Reçete ölçekleme: reçete artık "1 birim" varsayımı yerine açık bir baz
-- miktar/birim taşır. Kalem miktarları bu baz miktar içindir; üretim emri
-- N birim istediğinde hammaddeler N / bazMiktar oranıyla ölçeklenir.
-- Varsayılan 1 (mevcut tüm reçeteler eskisi gibi çalışır).

ALTER TABLE stok.recete
    ADD COLUMN IF NOT EXISTS baz_miktar NUMERIC(19,4) DEFAULT 1,
    ADD COLUMN IF NOT EXISTS baz_birim VARCHAR(20);
