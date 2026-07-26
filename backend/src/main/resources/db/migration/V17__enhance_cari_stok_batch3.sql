-- Cari: kredi limiti ve ödeme vadesi
ALTER TABLE cari.cari_hesap
  ADD COLUMN kredi_limiti DECIMAL(19,2),
  ADD COLUMN odeme_vadesi INTEGER DEFAULT 0;

-- Stok: multi-unit, tedarikçi, değerleme
ALTER TABLE stok.stok
  ADD COLUMN birim2 VARCHAR(50),
  ADD COLUMN cevrim_katsayisi DECIMAL(19,4),
  ADD COLUMN tedarikci_id BIGINT REFERENCES cari.cari_hesap(id),
  ADD COLUMN tedarikci_stok_kodu VARCHAR(100),
  ADD COLUMN tedarikci_fiyat DECIMAL(19,2),
  ADD COLUMN maliyet_yontemi VARCHAR(20) DEFAULT 'ORTALAMA';
