-- V106__donem_kilidi.sql
-- Donem kilidi ve yil sonu kapanis kaydi.
-- kilitli=true olan bir donemin tarih araligina giren belgeler (fatura vb.)
-- olusturulamaz/degistirilemez. Yil sonu kapanisi, donemi kilitler ve kapanis
-- ozetini kalici olarak kaydeder.

ALTER TABLE sistem.donem ADD COLUMN IF NOT EXISTS kilitli BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE sistem.donem ADD COLUMN IF NOT EXISTS kilit_tarihi TIMESTAMP;
ALTER TABLE sistem.donem ADD COLUMN IF NOT EXISTS kilit_kullanici_id BIGINT;

CREATE TABLE IF NOT EXISTS sistem.donem_kapanis (
    id               BIGSERIAL PRIMARY KEY,
    sirket_id        BIGINT NOT NULL,
    donem_id         BIGINT,
    yil              INTEGER NOT NULL,
    kapanis_tarihi   TIMESTAMP NOT NULL DEFAULT now(),
    kullanici_id     BIGINT,
    ozet             TEXT,
    CONSTRAINT fk_donem_kapanis_sirket FOREIGN KEY (sirket_id)
        REFERENCES sistem.sirket(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_donem_kapanis_sirket_yil
    ON sistem.donem_kapanis(sirket_id, yil);

CREATE INDEX IF NOT EXISTS idx_donem_kilit
    ON sistem.donem(sirket_id, kilitli);
