-- Faz 4: Veri bütünlüğü sertleştirmesi.
-- 1) Eksik yabancı anahtarlar (NOT VALID + VALIDATE). Mevcut satırlar doğrulanır;
--    ihlal varsa migration durur ve deploy bloke olur (sessiz bozulma önlenir).
-- 2) sirket_id -> sistem.sirket(id) tenant FK.
-- 3) Kritik çekirdek tablolarda sirket_id NOT NULL (tenant izolasyonu).
-- Not: Bu migration uygulanmadan önce mevcut veride yetim/null satır olmadığı doğrulandı.

-- ---------------------------------------------------------------------------
-- 1. Fatura bağlantıları
-- ---------------------------------------------------------------------------
ALTER TABLE fatura.fatura ADD CONSTRAINT fk_fatura_cari
    FOREIGN KEY (cari_hesap_id) REFERENCES cari.cari_hesap(id) NOT VALID;
ALTER TABLE fatura.fatura VALIDATE CONSTRAINT fk_fatura_cari;

ALTER TABLE fatura.fatura ADD CONSTRAINT fk_fatura_irsaliye
    FOREIGN KEY (irsaliye_id) REFERENCES muhasebe.irsaliye(id) NOT VALID;
ALTER TABLE fatura.fatura VALIDATE CONSTRAINT fk_fatura_irsaliye;

ALTER TABLE fatura.fatura ADD CONSTRAINT fk_fatura_kasa
    FOREIGN KEY (kasa_id) REFERENCES muhasebe.kasa(id) NOT VALID;
ALTER TABLE fatura.fatura VALIDATE CONSTRAINT fk_fatura_kasa;

ALTER TABLE fatura.fatura ADD CONSTRAINT fk_fatura_banka
    FOREIGN KEY (banka_id) REFERENCES muhasebe.banka(id) NOT VALID;
ALTER TABLE fatura.fatura VALIDATE CONSTRAINT fk_fatura_banka;

ALTER TABLE fatura.fatura ADD CONSTRAINT fk_fatura_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE fatura.fatura VALIDATE CONSTRAINT fk_fatura_sirket;

-- ---------------------------------------------------------------------------
-- 2. Banka hareketleri
-- ---------------------------------------------------------------------------
ALTER TABLE finans.banka_hareketi ADD CONSTRAINT fk_banka_hareketi_banka
    FOREIGN KEY (banka_id) REFERENCES muhasebe.banka(id) NOT VALID;
ALTER TABLE finans.banka_hareketi VALIDATE CONSTRAINT fk_banka_hareketi_banka;

ALTER TABLE finans.banka_hareketi ADD CONSTRAINT fk_banka_hareketi_kaynak_fatura
    FOREIGN KEY (kaynak_fatura_id) REFERENCES fatura.fatura(id) NOT VALID;
ALTER TABLE finans.banka_hareketi VALIDATE CONSTRAINT fk_banka_hareketi_kaynak_fatura;

ALTER TABLE finans.banka_hareketi ADD CONSTRAINT fk_banka_hareketi_eslesen_fatura
    FOREIGN KEY (eslesen_fatura_id) REFERENCES fatura.fatura(id) NOT VALID;
ALTER TABLE finans.banka_hareketi VALIDATE CONSTRAINT fk_banka_hareketi_eslesen_fatura;

ALTER TABLE finans.banka_hareketi ADD CONSTRAINT fk_banka_hareketi_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE finans.banka_hareketi VALIDATE CONSTRAINT fk_banka_hareketi_sirket;

-- ---------------------------------------------------------------------------
-- 3. Kasa hareketi / cari hareket fatura bağı
-- ---------------------------------------------------------------------------
ALTER TABLE muhasebe.kasa_hareket ADD CONSTRAINT fk_kasa_hareket_fatura
    FOREIGN KEY (fatura_id) REFERENCES fatura.fatura(id) NOT VALID;
ALTER TABLE muhasebe.kasa_hareket VALIDATE CONSTRAINT fk_kasa_hareket_fatura;

ALTER TABLE cari.hareket ADD CONSTRAINT fk_hareket_fatura
    FOREIGN KEY (fatura_id) REFERENCES fatura.fatura(id) NOT VALID;
ALTER TABLE cari.hareket VALIDATE CONSTRAINT fk_hareket_fatura;

-- ---------------------------------------------------------------------------
-- 4. Taksit planı
-- ---------------------------------------------------------------------------
ALTER TABLE finans.taksit ADD CONSTRAINT fk_taksit_fatura
    FOREIGN KEY (fatura_id) REFERENCES fatura.fatura(id) NOT VALID;
ALTER TABLE finans.taksit VALIDATE CONSTRAINT fk_taksit_fatura;

ALTER TABLE finans.taksit ADD CONSTRAINT fk_taksit_cari
    FOREIGN KEY (cari_id) REFERENCES cari.cari_hesap(id) NOT VALID;
ALTER TABLE finans.taksit VALIDATE CONSTRAINT fk_taksit_cari;

ALTER TABLE finans.taksit ADD CONSTRAINT fk_taksit_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE finans.taksit VALIDATE CONSTRAINT fk_taksit_sirket;

-- ---------------------------------------------------------------------------
-- 5. Sipariş / irsaliye
-- ---------------------------------------------------------------------------
ALTER TABLE siparis.siparis ADD CONSTRAINT fk_siparis_cari
    FOREIGN KEY (cari_hesap_id) REFERENCES cari.cari_hesap(id) NOT VALID;
ALTER TABLE siparis.siparis VALIDATE CONSTRAINT fk_siparis_cari;

ALTER TABLE siparis.siparis ADD CONSTRAINT fk_siparis_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE siparis.siparis VALIDATE CONSTRAINT fk_siparis_sirket;

ALTER TABLE muhasebe.irsaliye ADD CONSTRAINT fk_irsaliye_cari
    FOREIGN KEY (cari_hesap_id) REFERENCES cari.cari_hesap(id) NOT VALID;
ALTER TABLE muhasebe.irsaliye VALIDATE CONSTRAINT fk_irsaliye_cari;

ALTER TABLE muhasebe.irsaliye ADD CONSTRAINT fk_irsaliye_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE muhasebe.irsaliye VALIDATE CONSTRAINT fk_irsaliye_sirket;

-- ---------------------------------------------------------------------------
-- 6. Mevcut NOT VALID iade FK'sini doğrula (V54'te eklenmişti)
-- ---------------------------------------------------------------------------
ALTER TABLE ticaret.iade VALIDATE CONSTRAINT fk_iade_fatura;

-- ---------------------------------------------------------------------------
-- 7. Çekirdek tablolarda tenant FK (sirket_id -> sistem.sirket)
-- ---------------------------------------------------------------------------
ALTER TABLE stok.stok ADD CONSTRAINT fk_stok_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE stok.stok VALIDATE CONSTRAINT fk_stok_sirket;

ALTER TABLE cari.cari_hesap ADD CONSTRAINT fk_cari_hesap_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE cari.cari_hesap VALIDATE CONSTRAINT fk_cari_hesap_sirket;

ALTER TABLE cari.hareket ADD CONSTRAINT fk_hareket_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE cari.hareket VALIDATE CONSTRAINT fk_hareket_sirket;

ALTER TABLE muhasebe.kasa ADD CONSTRAINT fk_kasa_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE muhasebe.kasa VALIDATE CONSTRAINT fk_kasa_sirket;

ALTER TABLE muhasebe.banka ADD CONSTRAINT fk_banka_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE muhasebe.banka VALIDATE CONSTRAINT fk_banka_sirket;

ALTER TABLE ticaret.teklif ADD CONSTRAINT fk_teklif_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE ticaret.teklif VALIDATE CONSTRAINT fk_teklif_sirket;

ALTER TABLE muhasebe.cek_senet ADD CONSTRAINT fk_cek_senet_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE muhasebe.cek_senet VALIDATE CONSTRAINT fk_cek_senet_sirket;

ALTER TABLE personel.personel ADD CONSTRAINT fk_personel_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE personel.personel VALIDATE CONSTRAINT fk_personel_sirket;

ALTER TABLE proje.proje ADD CONSTRAINT fk_proje_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE proje.proje VALIDATE CONSTRAINT fk_proje_sirket;

ALTER TABLE sistem.gelir_gider_kategori ADD CONSTRAINT fk_gelir_gider_kategori_sirket
    FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID;
ALTER TABLE sistem.gelir_gider_kategori VALIDATE CONSTRAINT fk_gelir_gider_kategori_sirket;

-- ---------------------------------------------------------------------------
-- 8. Çekirdek tablolarda sirket_id NOT NULL (tenant izolasyonu)
-- ---------------------------------------------------------------------------
ALTER TABLE fatura.fatura ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE stok.stok ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE cari.cari_hesap ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE cari.hareket ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE muhasebe.kasa ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE muhasebe.banka ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE muhasebe.cek_senet ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE personel.personel ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE proje.proje ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE sistem.gelir_gider_kategori ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE ticaret.teklif ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE finans.banka_hareketi ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE siparis.siparis ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE muhasebe.irsaliye ALTER COLUMN sirket_id SET NOT NULL;
