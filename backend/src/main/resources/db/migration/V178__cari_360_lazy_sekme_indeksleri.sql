-- Cari 360 lazy sekmeleri için bileşik indeksler.
-- Not, fırsat, özel fiyat ve taksit listeleri cari + şirket + sıralama alanı
-- ile sayfalandığı için bu indeksler sorgu maliyetini düşürür.

CREATE INDEX IF NOT EXISTS idx_notlar_sirket_cari_tarih
    ON sistem.notlar (sirket_id, cari_hesap_id, olusturma_tarihi DESC);

CREATE INDEX IF NOT EXISTS idx_cari_firsat_sirket_cari_tarih
    ON ticaret.cari_firsat (sirket_id, cari_hesap_id, olusturma_tarihi DESC);

CREATE INDEX IF NOT EXISTS idx_cari_fiyat_sirket_cari_stok
    ON cari.cari_fiyat (sirket_id, cari_hesap_id, stok_id);

CREATE INDEX IF NOT EXISTS idx_taksit_sirket_cari_vade
    ON finans.taksit (sirket_id, cari_id, vade_tarihi);
