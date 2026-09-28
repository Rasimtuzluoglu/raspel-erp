-- V144__fk_butunlugu_ve_siparis_unique.sql
-- Amac: sonraki migration'larla eklenen referans kolonlarina veri butunlugu
-- kazandirmak. Oncesinde canli DB'de orphan taramasi yapildi (9 nullable kolonda
-- 0 orphan, mukerrer siparis_id yok).
--
-- Bilincli olarak FK EKLENMEYEN kolonlar:
--   * maliyet.stok_maliyet_hareket.stok_id : stok silindiginde maliyet defteri
--     tarihsel/denetlenebilir kalmalidir; FK stok silmeyi engellerdi.
--   * fatura.fatura_gecmis.fatura_id      : fatura silindiginde gecmis kaydi
--     korunmalidir (denetim izi); FK bunu engellerdi.
-- Bu kolonlar uygulama seviyesinde yonetilir.

-- 1) Orphan temizligi (idempotent; orphan yoksa 0 satir etkilenir) -------------
UPDATE sistem.bildirim b SET kullanici_id = NULL
 WHERE b.kullanici_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sistem.kullanici k WHERE k.id = b.kullanici_id);

UPDATE ticaret.iade i SET cari_hesap_id = NULL
 WHERE i.cari_hesap_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM cari.cari_hesap c WHERE c.id = i.cari_hesap_id);

UPDATE ticaret.iade i SET kasa_id = NULL
 WHERE i.kasa_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM muhasebe.kasa k WHERE k.id = i.kasa_id);

UPDATE ticaret.iade i SET banka_id = NULL
 WHERE i.banka_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM muhasebe.banka b WHERE b.id = i.banka_id);

UPDATE finans.masraf m SET kasa_id = NULL
 WHERE m.kasa_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM muhasebe.kasa k WHERE k.id = m.kasa_id);

UPDATE finans.masraf m SET banka_id = NULL
 WHERE m.banka_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM muhasebe.banka b WHERE b.id = m.banka_id);

UPDATE stok.stok_duzeltme d SET depo_id = NULL
 WHERE d.depo_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sube.depo s WHERE s.id = d.depo_id);

UPDATE fatura.fatura f SET pos_terminali_id = NULL
 WHERE f.pos_terminali_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM muhasebe.pos_terminali p WHERE p.id = f.pos_terminali_id);

UPDATE fatura.e_fatura e SET iade_id = NULL
 WHERE e.iade_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM ticaret.iade i WHERE i.id = e.iade_id);

-- 2) FK kolonlari icin index (Postgres FK'lerde referans kolonunu otomatik
--    indexlemez; silme/guncelleme performansi icin gerekir) -------------------
CREATE INDEX IF NOT EXISTS idx_iade_kasa ON ticaret.iade (kasa_id);
CREATE INDEX IF NOT EXISTS idx_iade_banka ON ticaret.iade (banka_id);
CREATE INDEX IF NOT EXISTS idx_masraf_kasa ON finans.masraf (kasa_id);
CREATE INDEX IF NOT EXISTS idx_masraf_banka ON finans.masraf (banka_id);
CREATE INDEX IF NOT EXISTS idx_stok_duzeltme_depo ON stok.stok_duzeltme (depo_id);

-- 3) Foreign key kisitlari (idempotent; ON DELETE SET NULL ile ana kayit
--    silindiginde referans null'lanir, kayit bloke edilmez) -------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_bildirim_kullanici') THEN
    ALTER TABLE sistem.bildirim
      ADD CONSTRAINT fk_bildirim_kullanici FOREIGN KEY (kullanici_id)
      REFERENCES sistem.kullanici(id) ON DELETE SET NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_iade_cari') THEN
    ALTER TABLE ticaret.iade
      ADD CONSTRAINT fk_iade_cari FOREIGN KEY (cari_hesap_id)
      REFERENCES cari.cari_hesap(id) ON DELETE SET NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_iade_kasa') THEN
    ALTER TABLE ticaret.iade
      ADD CONSTRAINT fk_iade_kasa FOREIGN KEY (kasa_id)
      REFERENCES muhasebe.kasa(id) ON DELETE SET NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_iade_banka') THEN
    ALTER TABLE ticaret.iade
      ADD CONSTRAINT fk_iade_banka FOREIGN KEY (banka_id)
      REFERENCES muhasebe.banka(id) ON DELETE SET NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_masraf_kasa') THEN
    ALTER TABLE finans.masraf
      ADD CONSTRAINT fk_masraf_kasa FOREIGN KEY (kasa_id)
      REFERENCES muhasebe.kasa(id) ON DELETE SET NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_masraf_banka') THEN
    ALTER TABLE finans.masraf
      ADD CONSTRAINT fk_masraf_banka FOREIGN KEY (banka_id)
      REFERENCES muhasebe.banka(id) ON DELETE SET NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_stok_duzeltme_depo') THEN
    ALTER TABLE stok.stok_duzeltme
      ADD CONSTRAINT fk_stok_duzeltme_depo FOREIGN KEY (depo_id)
      REFERENCES sube.depo(id) ON DELETE SET NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_fatura_pos_terminali') THEN
    ALTER TABLE fatura.fatura
      ADD CONSTRAINT fk_fatura_pos_terminali FOREIGN KEY (pos_terminali_id)
      REFERENCES muhasebe.pos_terminali(id) ON DELETE SET NULL;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_e_fatura_iade') THEN
    ALTER TABLE fatura.e_fatura
      ADD CONSTRAINT fk_e_fatura_iade FOREIGN KEY (iade_id)
      REFERENCES ticaret.iade(id) ON DELETE SET NULL;
  END IF;
END $$;

-- 4) Bir siparise yalnizca bir fatura baglanabilsin (savunmaci temizlik: mükerrer
--    baglantilarda en eski fatura korunur, digerlerinin bagi NULL'lanir) --------
UPDATE fatura.fatura f SET siparis_id = NULL
 WHERE f.siparis_id IS NOT NULL
   AND f.id <> (SELECT MIN(x.id) FROM fatura.fatura x WHERE x.siparis_id = f.siparis_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_fatura_siparis
    ON fatura.fatura (siparis_id) WHERE siparis_id IS NOT NULL;
