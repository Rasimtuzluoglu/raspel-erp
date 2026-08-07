-- V33__add_kullanici_sirket_many_to_many.sql
CREATE TABLE IF NOT EXISTS sistem.kullanici_sirket (
    kullanici_id BIGINT NOT NULL REFERENCES sistem.kullanici(id) ON DELETE CASCADE,
    sirket_id BIGINT NOT NULL REFERENCES sistem.sirket(id) ON DELETE CASCADE,
    PRIMARY KEY (kullanici_id, sirket_id)
);

-- Mevcut kullanicilari kendi sirket_id'lerine ata
INSERT INTO sistem.kullanici_sirket (kullanici_id, sirket_id)
SELECT id, sirket_id FROM sistem.kullanici WHERE sirket_id IS NOT NULL
ON CONFLICT DO NOTHING;
