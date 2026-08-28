-- V53: Tenant izolasyonu ve belge numarası bütünlüğü

-- 1) personel.personel_puantaj: sirket_id eklenir, mevcut kayıtlar personel üzerinden geri doldurulur
ALTER TABLE personel.personel_puantaj ADD COLUMN IF NOT EXISTS sirket_id BIGINT;

UPDATE personel.personel_puantaj pp
SET sirket_id = p.sirket_id
FROM personel.personel p
WHERE pp.personel_id = p.id
  AND pp.sirket_id IS NULL;

UPDATE personel.personel_puantaj SET sirket_id = 1 WHERE sirket_id IS NULL;

ALTER TABLE personel.personel_puantaj ALTER COLUMN sirket_id SET NOT NULL;
ALTER TABLE personel.personel_puantaj ADD CONSTRAINT fk_puantaj_sirket FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id);

CREATE INDEX IF NOT EXISTS idx_personel_puantaj_sirket ON personel.personel_puantaj(sirket_id);
CREATE INDEX IF NOT EXISTS idx_personel_puantaj_personel ON personel.personel_puantaj(personel_id);

-- 2) ticaret.teklif.teklif_no: şirket bazında benzersizlik (yarış koşulunda çift numara engeli)
-- Mevcut veride çift/boş teklif_no varsa migration patlamasın diye önce NULL'lar düzeltilir
UPDATE ticaret.teklif t SET teklif_no = 'TEKLIF-' || t.id
WHERE t.teklif_no IS NULL OR t.teklif_no = '';

DELETE FROM ticaret.teklif t
USING ticaret.teklif t2
WHERE t.id > t2.id
  AND t.sirket_id IS NOT DISTINCT FROM t2.sirket_id
  AND t.teklif_no = t2.teklif_no;

CREATE UNIQUE INDEX IF NOT EXISTS uk_teklif_no_sirket ON ticaret.teklif(sirket_id, teklif_no);

-- 3) muhasebe.muhasebe_fisi.fis_no: şirket bazında benzersizlik
DELETE FROM muhasebe.muhasebe_fisi f
USING muhasebe.muhasebe_fisi f2
WHERE f.id > f2.id
  AND f.sirket_id IS NOT DISTINCT FROM f2.sirket_id
  AND f.fis_no = f2.fis_no;

CREATE UNIQUE INDEX IF NOT EXISTS uk_fis_no_sirket ON muhasebe.muhasebe_fisi(sirket_id, fis_no);

-- 4) fatura.e_fatura.fatura_id: bir fatura için tek e-fatura
CREATE UNIQUE INDEX IF NOT EXISTS uk_efatura_fatura ON fatura.e_fatura(fatura_id);

-- 5) Eksik FK indeksleri (performans)
CREATE INDEX IF NOT EXISTS idx_satinalma_talep_kalem_talep ON satinalma.satinalma_talep_kalem(talep_id);
CREATE INDEX IF NOT EXISTS idx_satinalma_siparis_kalem_siparis ON satinalma.satinalma_siparis_kalem(siparis_id);
CREATE INDEX IF NOT EXISTS idx_stok_seri_stok ON envanter.stok_seri(stok_id);
CREATE INDEX IF NOT EXISTS idx_stok_sayim_stok ON envanter.stok_sayim(stok_id);
CREATE INDEX IF NOT EXISTS idx_depo_sube ON sube.depo(sube_id);
CREATE INDEX IF NOT EXISTS idx_irsaliye_kalem_irsaliye ON muhasebe.irsaliye_kalem(irsaliye_id);
CREATE INDEX IF NOT EXISTS idx_iade_fatura ON ticaret.iade(fatura_id);
