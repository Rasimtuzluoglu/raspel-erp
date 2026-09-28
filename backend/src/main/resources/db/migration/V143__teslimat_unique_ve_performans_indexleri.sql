-- V143__teslimat_unique_ve_performans_indexleri.sql
-- 1) Ayni faturaya birden fazla teslimat kaydi acilmasini DB seviyesinde engeller
--    (eszamanli iki istegin cift teslimat olusturmasini onler).
--    Not: Bu index olusturulmadan once mevcut mukerrer kayit bulunmamalidir;
--    deploy oncesi kontrol edildi.
CREATE UNIQUE INDEX IF NOT EXISTS uk_teslimat_sirket_fatura
    ON ticaret.teslimat (sirket_id, fatura_id)
    WHERE fatura_id IS NOT NULL;

-- Teslimat <-> fatura eslesmeleri ve sayfa sorgulari icin index.
CREATE INDEX IF NOT EXISTS idx_teslimat_fatura
    ON ticaret.teslimat (fatura_id);

-- 2) Denetim izi listeleme/filtreleme (sirket + tarih) icin bilesik index.
CREATE INDEX IF NOT EXISTS idx_audit_sirket_tarih
    ON sistem.audit_log (sirket_id, tarih DESC);

-- 3) Tahsilat merkezi ve vadesi gecen fatura sorgulari icin bilesik index.
CREATE INDEX IF NOT EXISTS idx_fatura_tahsilat
    ON fatura.fatura (sirket_id, durum, odeme_durumu, vade_tarihi);
