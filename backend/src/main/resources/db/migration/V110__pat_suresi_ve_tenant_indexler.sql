-- V110__pat_suresi_ve_tenant_indexler.sql
-- 1) Kisisel API token'lari icin son kullanma tarihi ve token surumu (parola
--    degisiminde otomatik gecersizlik) alanlari.
-- 2) Sik filtrelenen tenant/tarih/FK kolonlari icin eksik indeksler.

ALTER TABLE sistem.api_token ADD COLUMN IF NOT EXISTS son_kullanma TIMESTAMP;
ALTER TABLE sistem.api_token ADD COLUMN IF NOT EXISTS token_version BIGINT;
CREATE INDEX IF NOT EXISTS idx_api_token_kullanici ON sistem.api_token(kullanici_id);

CREATE INDEX IF NOT EXISTS idx_belge_sirket ON sistem.belge(sirket_id);
CREATE INDEX IF NOT EXISTS idx_kullanici_sirket ON sistem.kullanici(sirket_id);
CREATE INDEX IF NOT EXISTS idx_kullanici_sirket_tablo ON sistem.kullanici_sirket(sirket_id);
CREATE INDEX IF NOT EXISTS idx_onay_ayari_sirket ON sistem.onay_ayari(sirket_id);
CREATE INDEX IF NOT EXISTS idx_ajanda_gorev_sirket ON sistem.ajanda_gorev(sirket_id);
CREATE INDEX IF NOT EXISTS idx_ajanda_hatirlatici_sirket ON sistem.ajanda_hatirlatici(sirket_id);
CREATE INDEX IF NOT EXISTS idx_hata_log_sirket ON sistem.hata_log(sirket_id);
CREATE INDEX IF NOT EXISTS idx_ai_config_sirket ON sistem.ai_config(sirket_id);

CREATE INDEX IF NOT EXISTS idx_cari_fiyat_sirket ON cari.cari_fiyat(sirket_id);

CREATE INDEX IF NOT EXISTS idx_stok_fiyat_sirket ON stok.stok_fiyat(sirket_id);
CREATE INDEX IF NOT EXISTS idx_stok_hareket_cari ON stok.stok_hareket(cari_hesap_id);

CREATE INDEX IF NOT EXISTS idx_sube_depo_sirket ON sube.depo(sirket_id);
CREATE INDEX IF NOT EXISTS idx_sube_sube_sirket ON sube.sube(sirket_id);
CREATE INDEX IF NOT EXISTS idx_depo_stok_stok ON sube.depo_stok(stok_id);

CREATE INDEX IF NOT EXISTS idx_proje_proje_sirket ON proje.proje(sirket_id);

CREATE INDEX IF NOT EXISTS idx_envanter_stok_sayim_sirket ON envanter.stok_sayim(sirket_id);

CREATE INDEX IF NOT EXISTS idx_ik_maas_bordro_sirket ON ik.maas_bordro(sirket_id);
CREATE INDEX IF NOT EXISTS idx_ik_vardiya_sirket ON ik.vardiya(sirket_id);

CREATE INDEX IF NOT EXISTS idx_muhasebe_cek_senet_sirket ON muhasebe.cek_senet(sirket_id);
CREATE INDEX IF NOT EXISTS idx_muhasebe_irsaliye_cari ON muhasebe.irsaliye(cari_hesap_id);

CREATE INDEX IF NOT EXISTS idx_finans_masraf_tarih ON finans.masraf(tarih);

CREATE INDEX IF NOT EXISTS idx_siparis_cari ON siparis.siparis(cari_hesap_id);
CREATE INDEX IF NOT EXISTS idx_siparis_kalem_stok ON siparis.siparis_kalem(stok_id);

CREATE INDEX IF NOT EXISTS idx_satinalma_siparis_cari ON satinalma.satinalma_siparis(cari_hesap_id);

CREATE INDEX IF NOT EXISTS idx_ticaret_fiyat_listesi_sirket ON ticaret.fiyat_listesi(sirket_id);
CREATE INDEX IF NOT EXISTS idx_ticaret_teklif_kalem_stok ON ticaret.teklif_kalem(stok_id);

-- POS gun sonu ayni POS icin ayni gunde yalnizca bir kez islenir (idempotentlik yarisi).
CREATE UNIQUE INDEX IF NOT EXISTS uk_pos_gun_sonu_pos_tarih ON muhasebe.pos_gun_sonu(pos_id, tarih);
