-- V113__seri_sayac_tablosu.sql
-- Belge numaralari (fatura/siparis/teklif) uygulama tarafinda uretilirken es zamanli
-- istekler ayni numarayi uretip UNIQUE ihlali verebiliyordu. Bu tablo, numara
-- uretimini atomik hale getirir: INSERT ... ON CONFLICT DO UPDATE ... RETURNING.

CREATE TABLE IF NOT EXISTS sistem.seri_sayac (
    sirket_id BIGINT      NOT NULL,
    tur       VARCHAR(20) NOT NULL,
    deger     INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT pk_seri_sayac PRIMARY KEY (sirket_id, tur)
);
