CREATE TABLE IF NOT EXISTS ticaret.teklif (
    id BIGSERIAL PRIMARY KEY,
    teklif_no VARCHAR(50) NOT NULL,
    revizyon_no INT NOT NULL DEFAULT 0,
    tarih DATE NOT NULL,
    gecerlilik_tarihi DATE,
    cari_hesap_id BIGINT,
    tur VARCHAR(20) DEFAULT 'SATIS',
    durum VARCHAR(30) NOT NULL DEFAULT 'TASLAK',
    ara_toplam NUMERIC(19, 2) DEFAULT 0,
    kdv NUMERIC(19, 2) DEFAULT 0,
    iskonto_orani NUMERIC(5, 2) DEFAULT 0,
    iskonto_tutari NUMERIC(19, 2) DEFAULT 0,
    genel_toplam NUMERIC(19, 2) DEFAULT 0,
    para_birimi VARCHAR(10) DEFAULT 'TRY',
    teslimat_sarti VARCHAR(255),
    odeme_sarti VARCHAR(255),
    garanti_sarti VARCHAR(255),
    notlar TEXT,
    sirket_id BIGINT,
    olusturma_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    guncelleme_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_teklif_sirket ON ticaret.teklif(sirket_id);
CREATE INDEX IF NOT EXISTS idx_teklif_cari ON ticaret.teklif(cari_hesap_id);
CREATE INDEX IF NOT EXISTS idx_teklif_durum ON ticaret.teklif(durum);

CREATE TABLE IF NOT EXISTS ticaret.teklif_kalem (
    id BIGSERIAL PRIMARY KEY,
    teklif_id BIGINT NOT NULL,
    stok_id BIGINT,
    aciklama VARCHAR(500) NOT NULL,
    miktar NUMERIC(19, 2) NOT NULL DEFAULT 1,
    birim VARCHAR(20) DEFAULT 'Adet',
    birim_fiyat NUMERIC(19, 2) NOT NULL DEFAULT 0,
    iskonto_orani NUMERIC(5, 2) DEFAULT 0,
    kdv_orani NUMERIC(5, 2) DEFAULT 20,
    tutar NUMERIC(19, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_teklif_kalem_teklif FOREIGN KEY (teklif_id) REFERENCES ticaret.teklif(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_teklif_kalem_teklif ON ticaret.teklif_kalem(teklif_id);
