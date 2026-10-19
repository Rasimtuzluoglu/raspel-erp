-- =====================================================================
-- V166: Anomali inceleme durumu
--
-- KULLANICI TALEBI:
--   "Anomali kartlarinda 'incelendi / yok say' aksiyonu kalici olsun."
--
-- TASARIM:
--   1) Anomaliler her taramada yeniden uretilir; bu yuzden kalici anahtar
--      deterministik olmalidir (tur + ilgili kayit id'sinden UUID).
--   2) Anahtar sirket bazinda benzersizdir; ayni bulgu tekrar tarandiginda
--      ayni kayit guncellenir.
--   3) durum: INCELENDI | YOK_SAYILDI. Kayit yoksa bulgu "acik" kabul edilir.
--
-- Idempotent.
-- =====================================================================

CREATE TABLE IF NOT EXISTS sistem.anomali_durum (
    id                BIGSERIAL PRIMARY KEY,
    sirket_id         BIGINT       NOT NULL,
    anahtar           VARCHAR(64)  NOT NULL,
    durum             VARCHAR(20)  NOT NULL,
    kullanici_id      BIGINT,
    olusturma_tarihi  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_anomali_durum_sirket_anahtar UNIQUE (sirket_id, anahtar)
);

CREATE INDEX IF NOT EXISTS idx_anomali_durum_sirket
    ON sistem.anomali_durum (sirket_id);

DO $$
DECLARE
    v_tablo  bigint;
    v_indeks bigint;
BEGIN
    SELECT count(*) INTO v_tablo FROM information_schema.tables
    WHERE table_schema = 'sistem' AND table_name = 'anomali_durum';
    IF v_tablo = 0 THEN
        RAISE EXCEPTION 'V166 HATA: sistem.anomali_durum tablosu olusturulamadi';
    END IF;

    SELECT count(*) INTO v_indeks FROM pg_indexes
    WHERE schemaname = 'sistem' AND indexname LIKE 'idx_anomali_durum%';
    IF v_indeks < 1 THEN
        RAISE EXCEPTION 'V166 HATA: anomali_durum indeksi olusturulamadi';
    END IF;

    RAISE NOTICE 'V166: sistem.anomali_durum olusturuldu';
END $$;
