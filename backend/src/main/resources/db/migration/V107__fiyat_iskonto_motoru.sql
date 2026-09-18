-- V107__fiyat_iskonto_motoru.sql
-- Gelismis fiyat/iskonto motoru: kademeli (miktar bazli) iskonto kurallari.
-- Bir kural; stok (opsiyonel), cari hesap (opsiyonel), kategori (opsiyonel),
-- tarih araligi ve miktar araligi ile kapsamlanir. Oncelik sirasina gore
-- en uygun (en yuksek oncelikli, en yuksek oranli) kural uygulanir.

CREATE TABLE IF NOT EXISTS ticaret.iskonto_kurali (
    id               BIGSERIAL PRIMARY KEY,
    sirket_id        BIGINT NOT NULL,
    ad               VARCHAR(150) NOT NULL,
    stok_id          BIGINT,
    cari_hesap_id    BIGINT,
    kategori         VARCHAR(100),
    min_adet         NUMERIC(19, 4),
    max_adet         NUMERIC(19, 4),
    iskonto_orani    NUMERIC(9, 2) NOT NULL,
    oncelik          INTEGER NOT NULL DEFAULT 100,
    gecerli_baslangic DATE,
    gecerli_bitis    DATE,
    aktif            BOOLEAN NOT NULL DEFAULT true,
    aciklama         VARCHAR(500),
    olusturma_tarihi TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_iskonto_kurali_sirket
    ON ticaret.iskonto_kurali(sirket_id, aktif);

CREATE INDEX IF NOT EXISTS idx_iskonto_kurali_stok
    ON ticaret.iskonto_kurali(stok_id);
