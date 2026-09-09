-- V70__add_ajanda_gorev_hatirlatici.sql
-- Kişisel ajanda görevleri ve hatırlatıcıları.
CREATE TABLE IF NOT EXISTS sistem.ajanda_gorev (
    id BIGSERIAL PRIMARY KEY,
    kullanici_id BIGINT NOT NULL,
    sirket_id BIGINT,
    baslik VARCHAR(200) NOT NULL,
    aciklama VARCHAR(1000),
    bitis_tarihi DATE,
    oncelik VARCHAR(20) NOT NULL DEFAULT 'ORTA',
    durum VARCHAR(20) NOT NULL DEFAULT 'BEKLIYOR',
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ajanda_gorev_kullanici ON sistem.ajanda_gorev (kullanici_id);
CREATE INDEX IF NOT EXISTS idx_ajanda_gorev_tarih ON sistem.ajanda_gorev (bitis_tarihi);

CREATE TABLE IF NOT EXISTS sistem.ajanda_hatirlatici (
    id BIGSERIAL PRIMARY KEY,
    kullanici_id BIGINT NOT NULL,
    sirket_id BIGINT,
    gorev_id BIGINT,
    baslik VARCHAR(200) NOT NULL,
    hatirlatma_zamani TIMESTAMP NOT NULL,
    bildirildi BOOLEAN NOT NULL DEFAULT false,
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ajanda_hatirlatici_kullanici ON sistem.ajanda_hatirlatici (kullanici_id);
CREATE INDEX IF NOT EXISTS idx_ajanda_hatirlatici_zaman ON sistem.ajanda_hatirlatici (hatirlatma_zamani);
