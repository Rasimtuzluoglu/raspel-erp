-- Ek performans indeksleri: tarih siralamali/taranan sicak kolonlar.
-- stok_hareket: her okuma hareket_tarihi'ne gore siralanir (stok bazli gecmis).
CREATE INDEX IF NOT EXISTS idx_stok_hareket_stok_tarih
    ON stok.stok_hareket (stok_id, hareket_tarihi DESC);

-- cari.hareket: tenant + tarih araligi filtreleri ve tarih siralamasi.
CREATE INDEX IF NOT EXISTS idx_hareket_sirket_tarih
    ON cari.hareket (sirket_id, hareket_tarihi DESC);

-- cari.hareket: POS terminaline gore hareket listeleme.
CREATE INDEX IF NOT EXISTS idx_hareket_pos_terminali
    ON cari.hareket (pos_terminali_id);
