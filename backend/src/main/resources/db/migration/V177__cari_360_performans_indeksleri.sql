-- Cari 360 kartı ve cari liste sorguları için bileşik indeksler.
-- Mevcut tek kolonlu indeksler korunur; bu indeksler cari + şirket + tarih
-- birlikte filtrelendiğinde sıralama ve son N kayıt okumasını hızlandırır.

CREATE INDEX IF NOT EXISTS idx_fatura_cari_sirket_tarih
    ON fatura.fatura (cari_hesap_id, sirket_id, tarih DESC);

CREATE INDEX IF NOT EXISTS idx_siparis_cari_sirket_tarih
    ON siparis.siparis (cari_hesap_id, sirket_id, tarih DESC);

CREATE INDEX IF NOT EXISTS idx_iade_cari_sirket_tarih
    ON ticaret.iade (cari_hesap_id, sirket_id, tarih DESC);

CREATE INDEX IF NOT EXISTS idx_hareket_cari_sirket_tarih
    ON cari.hareket (cari_hesap_id, sirket_id, hareket_tarihi DESC);
