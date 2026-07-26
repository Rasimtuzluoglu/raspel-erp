-- Fatura: indirim ve ödeme takibi alanları
ALTER TABLE fatura.fatura
  ADD COLUMN genel_iskonto_tutari DECIMAL(19,2) DEFAULT 0 NOT NULL,
  ADD COLUMN odeme_durumu VARCHAR(20) DEFAULT 'ODENMEDI' NOT NULL,
  ADD COLUMN odenen_tutar DECIMAL(19,2) DEFAULT 0 NOT NULL,
  ADD COLUMN kalan_tutar DECIMAL(19,2) DEFAULT 0 NOT NULL;

-- FaturaKalem: satır iskonto oranı
ALTER TABLE fatura.fatura_kalem
  ADD COLUMN iskonto_orani DECIMAL(5,2) DEFAULT 0 NOT NULL;

-- Hareket: ödeme şekli
ALTER TABLE cari.hareket
  ADD COLUMN odeme_sekli VARCHAR(20);

-- IadeKalem: iade kalemleri tablosu
CREATE TABLE ticaret.iade_kalem (
  id BIGSERIAL PRIMARY KEY,
  iade_id BIGINT NOT NULL REFERENCES ticaret.iade(id) ON DELETE CASCADE,
  stok_id BIGINT REFERENCES stok.stok(id),
  aciklama VARCHAR(300),
  miktar DECIMAL(19,2) NOT NULL DEFAULT 1,
  birim VARCHAR(50),
  birim_fiyat DECIMAL(19,2) NOT NULL DEFAULT 0,
  kdv_orani DECIMAL(5,2) DEFAULT 20,
  tutar DECIMAL(19,2) NOT NULL DEFAULT 0,
  olusturma_tarihi TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
