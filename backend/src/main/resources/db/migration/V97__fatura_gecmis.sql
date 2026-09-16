-- V97__fatura_gecmis.sql
-- Fatura duzenleme / durum degisikligi / silme ve yazdirma izini tek birlesik
-- gecmis tablosunda tutar. Satis ve alis faturalari ayni tabloyu paylasir (tur alani faturada).
CREATE TABLE IF NOT EXISTS fatura.fatura_gecmis (
    id              BIGSERIAL PRIMARY KEY,
    fatura_id       BIGINT       NOT NULL,
    sirket_id       BIGINT,
    olay            VARCHAR(20)  NOT NULL,
    aciklama        VARCHAR(500),
    onceki_deger    TEXT,
    yeni_deger      TEXT,
    kullanici_id    BIGINT,
    kullanici_adi   VARCHAR(100),
    ip_adresi       VARCHAR(50),
    yazdirma_format VARCHAR(20),
    yazici_adi      VARCHAR(150),
    kopya_no        INTEGER,
    tarih           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fatura_gecmis_fatura ON fatura.fatura_gecmis(fatura_id, tarih DESC);
CREATE INDEX IF NOT EXISTS idx_fatura_gecmis_sirket ON fatura.fatura_gecmis(sirket_id, tarih DESC);
CREATE INDEX IF NOT EXISTS idx_fatura_gecmis_olay ON fatura.fatura_gecmis(olay);
