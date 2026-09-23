-- Faz 4 (genisletme): Satir/append-only entity'lerine iyimser kilitleme.
-- Fatura/iade/siparis/irsaliye kalemleri, stok seri/sayim/maliyet/fiyat,
-- cari fiyat, cek-senet, masraf ve butce kayitlarina @Version eklenir.

ALTER TABLE fatura.fatura_kalem            ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ticaret.iade_kalem             ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE siparis.siparis_kalem          ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE muhasebe.irsaliye_kalem        ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE envanter.stok_seri             ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE envanter.stok_sayim            ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE maliyet.stok_maliyet_hareket   ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE stok.stok_fiyat                ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cari.cari_fiyat                ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE muhasebe.cek_senet             ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE finans.masraf                  ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE finans.butce                   ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
