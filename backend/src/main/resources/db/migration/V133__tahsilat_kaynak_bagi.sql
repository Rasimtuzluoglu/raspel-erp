-- Tahsilat -> kasa/banka hareket kaynak baglantisi.
-- Tahsilat silindiginde/iptal edildiginde bagli kasa/banka hareketi de ters kaydedilebilsin
-- diye cari hareket kimligi saklanir (kaynak_tip='TAHSILAT', kaynak_id=<cari hareket id>).
ALTER TABLE muhasebe.kasa_hareket ADD COLUMN IF NOT EXISTS kaynak_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_kasa_hareket_kaynak ON muhasebe.kasa_hareket (kaynak_tip, kaynak_id);

ALTER TABLE finans.banka_hareketi ADD COLUMN IF NOT EXISTS kaynak_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_banka_hareket_kaynak ON finans.banka_hareketi (kaynak_tip, kaynak_id);
