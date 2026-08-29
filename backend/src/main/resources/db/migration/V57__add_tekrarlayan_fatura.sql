-- V57__add_tekrarlayan_fatura.sql
-- Tekrarlayan (periyodik) fatura tanımları ve kalemleri.
CREATE TABLE IF NOT EXISTS fatura.tekrarlayan_fatura (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT NOT NULL,
    cari_hesap_id BIGINT,
    tur VARCHAR(10) NOT NULL DEFAULT 'SATIS',
    aciklama VARCHAR(500),
    periyot VARCHAR(20) NOT NULL,
    baslangic_tarihi DATE NOT NULL,
    bitis_tarihi DATE,
    sonraki_calistirma DATE,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    olusturma_tarihi TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS fatura.tekrarlayan_fatura_kalem (
    id BIGSERIAL PRIMARY KEY,
    tekrarlayan_fatura_id BIGINT NOT NULL REFERENCES fatura.tekrarlayan_fatura(id) ON DELETE CASCADE,
    aciklama VARCHAR(300) NOT NULL,
    adet INTEGER NOT NULL,
    birim_fiyat NUMERIC(19,2) NOT NULL,
    kdv_orani NUMERIC(5,2) DEFAULT 0,
    iskonto_orani NUMERIC(5,2) DEFAULT 0,
    stok_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_tekrarlayan_fatura_sirket ON fatura.tekrarlayan_fatura(sirket_id);
CREATE INDEX IF NOT EXISTS idx_tekrarlayan_fatura_kalem_fk ON fatura.tekrarlayan_fatura_kalem(tekrarlayan_fatura_id);
