-- V94__stok_hareket_iz.sql
-- Stok hareketlerinin kaynagini (belge tipi/id), deposunu ve serisini izlenebilir kilar.
-- Eski kayitlarda bu alanlar NULL kalir (geriye donuk uyumlu).
ALTER TABLE stok.stok_hareket ADD COLUMN IF NOT EXISTS depo_id BIGINT;
ALTER TABLE stok.stok_hareket ADD COLUMN IF NOT EXISTS kaynak_tip VARCHAR(30);
ALTER TABLE stok.stok_hareket ADD COLUMN IF NOT EXISTS kaynak_id BIGINT;
ALTER TABLE stok.stok_hareket ADD COLUMN IF NOT EXISTS seri_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_stok_hareket_depo ON stok.stok_hareket(depo_id);
CREATE INDEX IF NOT EXISTS idx_stok_hareket_kaynak ON stok.stok_hareket(kaynak_tip, kaynak_id);
CREATE INDEX IF NOT EXISTS idx_stok_hareket_seri ON stok.stok_hareket(seri_id);
