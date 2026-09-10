-- V80__add_pos_gun_sonu.sql
-- POS gün sonu: gün içinde POS'tan çekilen tutarların bankaya aktarım kaydı.
CREATE TABLE IF NOT EXISTS muhasebe.pos_gun_sonu (
    id BIGSERIAL PRIMARY KEY,
    pos_id BIGINT NOT NULL,
    sirket_id BIGINT,
    tutar NUMERIC(19,2) NOT NULL DEFAULT 0,
    komisyon NUMERIC(19,2) NOT NULL DEFAULT 0,
    tarih DATE NOT NULL,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (pos_id, tarih)
);

CREATE INDEX IF NOT EXISTS idx_pos_gun_sonu_sirket ON muhasebe.pos_gun_sonu (sirket_id, tarih);
