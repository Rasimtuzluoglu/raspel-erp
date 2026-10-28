-- =====================================================================
-- V172: Onay karar geçmişi
--
-- Amaç:
--   - İzin, masraf/avans, satınalma ve sipariş onaylarını tek ekranda
--     yönetebilmek için Birleşik Onay API'sinin karar kayıtlarını tutmak.
--   - Her karar; tür, kaynak kayıt, karar (ONAY/RED), gerekçe, durum
--     geçişi ve karar veren kullanıcı bilgisiyle denetlenebilir olur.
--
-- Notlar:
--   - Yalnızca YENİ kararlar yazılır; geriye dönük backfill yapılmaz.
--   - Modül işlemi başarılı olduktan sonra yazılır (hata varsa karar yok).
--
-- Idempotent: tablo ve indeks oluşturmaları IF NOT EXISTS ile korunur.
-- =====================================================================

CREATE TABLE IF NOT EXISTS sistem.onay_karar (
    id               BIGSERIAL PRIMARY KEY,
    sirket_id        BIGINT       NOT NULL,
    tur              VARCHAR(20)  NOT NULL, -- IZIN, MASRAF, SATINALMA, SIPARIS
    kaynak_id        BIGINT       NOT NULL,
    karar            VARCHAR(10)  NOT NULL, -- ONAY, RED
    gerekce          VARCHAR(500),
    onceki_durum     VARCHAR(40),
    yeni_durum       VARCHAR(40),
    karar_veren_id   BIGINT,
    karar_veren_ad   VARCHAR(150),
    olusturma_tarihi TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Geçmiş listesi: şirket + en yeni karar önce.
CREATE INDEX IF NOT EXISTS idx_onay_karar_sirket_tarih
    ON sistem.onay_karar (sirket_id, olusturma_tarihi DESC);

-- Bir kaydın karar geçmişi (kaynak kaydı bazında).
CREATE INDEX IF NOT EXISTS idx_onay_karar_tur_kaynak
    ON sistem.onay_karar (tur, kaynak_id);
