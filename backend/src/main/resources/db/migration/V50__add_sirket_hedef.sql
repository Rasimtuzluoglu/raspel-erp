CREATE TABLE IF NOT EXISTS sistem.sirket_hedef (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT NOT NULL,
    yil INT NOT NULL,
    ay INT NOT NULL,
    hedef_ciro NUMERIC(19, 2) NOT NULL DEFAULT 0,
    hedef_kar NUMERIC(19, 2) DEFAULT 0,
    hedef_yeni_musteri INT DEFAULT 0,
    hedef_satis_adedi INT DEFAULT 0,
    notlar VARCHAR(500),
    guncelleme_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sirket_hedef_yil_ay UNIQUE (sirket_id, yil, ay)
);

CREATE INDEX IF NOT EXISTS idx_sirket_hedef ON sistem.sirket_hedef(sirket_id, yil, ay);
