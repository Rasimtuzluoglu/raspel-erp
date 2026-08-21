CREATE TABLE IF NOT EXISTS ik.personel_masraf_talep (
    id BIGSERIAL PRIMARY KEY,
    personel_id BIGINT,
    kullanici_id BIGINT,
    sirket_id BIGINT NOT NULL,
    tur VARCHAR(20) NOT NULL DEFAULT 'MASRAF',
    kategori VARCHAR(50) NOT NULL DEFAULT 'DIGER',
    tutar NUMERIC(19, 2) NOT NULL DEFAULT 0,
    para_birimi VARCHAR(10) DEFAULT 'TRY',
    tarih DATE NOT NULL,
    aciklama VARCHAR(500) NOT NULL,
    belge_url TEXT,
    durum VARCHAR(30) NOT NULL DEFAULT 'BEKLEMEDE',
    onaylayan VARCHAR(100),
    onay_notu VARCHAR(500),
    olusturma_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    guncelleme_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_personel_masraf_sirket ON ik.personel_masraf_talep(sirket_id);
CREATE INDEX IF NOT EXISTS idx_personel_masraf_personel ON ik.personel_masraf_talep(personel_id);
CREATE INDEX IF NOT EXISTS idx_personel_masraf_durum ON ik.personel_masraf_talep(durum);
