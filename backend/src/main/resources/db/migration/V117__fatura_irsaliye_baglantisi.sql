-- Fatura: dayandığı irsaliye bağlantısı (irsaliye zaten stok işlediyse çift düşümü önler).
ALTER TABLE fatura.fatura ADD COLUMN IF NOT EXISTS irsaliye_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_fatura_irsaliye ON fatura.fatura (irsaliye_id);

-- POS gün sonu idempotentliği: aynı POS+tarih için ikinci kayıt engellenir (yarış koşulu koruması).
-- Mevcut veride olası mükerrer kayıtlar temizlenir.
DELETE FROM muhasebe.pos_gun_sonu a
    USING muhasebe.pos_gun_sonu b
    WHERE a.id < b.id AND a.pos_id = b.pos_id AND a.tarih = b.tarih;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pos_gun_sonu_pos_tarih ON muhasebe.pos_gun_sonu (pos_id, tarih);
