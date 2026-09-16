-- V98__maliyet_motoru.sql
-- Agirlikli ortalama maliyet (COGS) motoru:
--   * stok.ortalama_maliyet  : guncel agirlikli ortalama birim maliyet (4 hane)
--   * fatura.fatura_kalem    : satis anindaki birim maliyet / toplam maliyet anlik goruntusu
--   * maliyet.stok_maliyet_hareket : denetlenebilir maliyet defteri (giris/cikis)
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS ortalama_maliyet NUMERIC(19, 4);

UPDATE stok.stok
   SET ortalama_maliyet = ROUND(COALESCE(NULLIF(tedarikci_fiyat, 0), fiyat, 0)::numeric, 4)
 WHERE ortalama_maliyet IS NULL;

UPDATE stok.stok
   SET maliyet_yontemi = 'AGIRLIKLI_ORTALAMA'
 WHERE maliyet_yontemi IS NULL OR maliyet_yontemi = '';

ALTER TABLE fatura.fatura_kalem ADD COLUMN IF NOT EXISTS birim_maliyet NUMERIC(19, 4);
ALTER TABLE fatura.fatura_kalem ADD COLUMN IF NOT EXISTS maliyet_tutar NUMERIC(19, 2);

CREATE SCHEMA IF NOT EXISTS maliyet;

CREATE TABLE IF NOT EXISTS maliyet.stok_maliyet_hareket (
    id                BIGSERIAL PRIMARY KEY,
    stok_id           BIGINT         NOT NULL,
    sirket_id         BIGINT,
    tarih             DATE           NOT NULL,
    tur               VARCHAR(10)    NOT NULL,
    miktar            NUMERIC(19, 4) NOT NULL,
    birim_maliyet     NUMERIC(19, 4) NOT NULL,
    toplam_maliyet    NUMERIC(19, 2) NOT NULL,
    kalan_miktar      NUMERIC(19, 4) NOT NULL,
    ortalama_maliyet  NUMERIC(19, 4) NOT NULL,
    kaynak_tip        VARCHAR(30),
    kaynak_id         BIGINT,
    olusturma_tarihi  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_smh_stok ON maliyet.stok_maliyet_hareket(stok_id, tarih);
CREATE INDEX IF NOT EXISTS idx_smh_sirket ON maliyet.stok_maliyet_hareket(sirket_id, tarih);
