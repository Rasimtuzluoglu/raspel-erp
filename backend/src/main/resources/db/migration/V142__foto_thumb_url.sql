-- Ürün/cari görselleri için küçük (thumbnail) varyant adresi.
-- Liste ve kart görünümleri thumbnail kullanır; detayda büyük (sıkıştırılmış) görsel gösterilir.
ALTER TABLE stok.stok ADD COLUMN IF NOT EXISTS foto_thumb_url VARCHAR(500);
ALTER TABLE cari.cari_hesap ADD COLUMN IF NOT EXISTS foto_thumb_url VARCHAR(500);
