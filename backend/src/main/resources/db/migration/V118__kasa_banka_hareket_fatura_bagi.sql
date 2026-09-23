-- Para izi: kasa/banka hareketlerinin kaynak faturaya bağlanması.
-- Böylece bir satışın nakit/banka hareketi fatura numarasından izlenebilir ve
-- fatura iptalinde bu hareketler güvenle geri alınabilir.
ALTER TABLE muhasebe.kasa_hareket ADD COLUMN IF NOT EXISTS fatura_id BIGINT;
ALTER TABLE muhasebe.kasa_hareket ADD COLUMN IF NOT EXISTS kaynak_tip VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_kasa_hareket_fatura ON muhasebe.kasa_hareket (fatura_id);

ALTER TABLE finans.banka_hareketi ADD COLUMN IF NOT EXISTS kaynak_fatura_id BIGINT;
ALTER TABLE finans.banka_hareketi ADD COLUMN IF NOT EXISTS kaynak_tip VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_banka_hareketi_kaynak_fatura ON finans.banka_hareketi (kaynak_fatura_id);
