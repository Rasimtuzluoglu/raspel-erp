-- Performans iyileştirmeleri: en çok sorgulanan kolonlara index'ler

-- Cari hesaplar
CREATE INDEX IF NOT EXISTS idx_cari_sirket ON cari.cari_hesap(sirket_id);
CREATE INDEX IF NOT EXISTS idx_cari_aktif ON cari.cari_hesap(aktif);

-- Faturalar
CREATE INDEX IF NOT EXISTS idx_fatura_sirket_tarih ON fatura.fatura(sirket_id, tarih);
CREATE INDEX IF NOT EXISTS idx_fatura_durum ON fatura.fatura(durum);
CREATE INDEX IF NOT EXISTS idx_fatura_cari ON fatura.fatura(cari_hesap_id);
CREATE INDEX IF NOT EXISTS idx_fatura_kalem_fatura ON fatura.fatura_kalem(fatura_id);

-- Hareketler (cari şeması)
CREATE INDEX IF NOT EXISTS idx_hareket_cari_tarih ON cari.hareket(cari_hesap_id, hareket_tarihi);
CREATE INDEX IF NOT EXISTS idx_hareket_tur_tarih ON cari.hareket(tur, hareket_tarihi);

-- Stoklar
CREATE INDEX IF NOT EXISTS idx_stok_sirket ON stok.stok(sirket_id);
CREATE INDEX IF NOT EXISTS idx_stok_barkod ON stok.stok(barkod);
CREATE INDEX IF NOT EXISTS idx_stok_hareket_stok ON stok.stok_hareket(stok_id);
CREATE INDEX IF NOT EXISTS idx_stok_hareket_tur ON stok.stok_hareket(tur);

-- Siparişler
CREATE INDEX IF NOT EXISTS idx_siparis_sirket_tarih ON siparis.siparis(sirket_id, tarih);
CREATE INDEX IF NOT EXISTS idx_siparis_durum ON siparis.siparis(durum);
CREATE INDEX IF NOT EXISTS idx_siparis_kalem_siparis ON siparis.siparis_kalem(siparis_id);

-- İrsaliyeler (muhasebe şeması)
CREATE INDEX IF NOT EXISTS idx_irsaliye_sirket_tarih ON muhasebe.irsaliye(sirket_id, tarih);

-- Personel
CREATE INDEX IF NOT EXISTS idx_personel_sirket ON personel.personel(sirket_id);
CREATE INDEX IF NOT EXISTS idx_personel_aktif ON personel.personel(aktif);

-- İadeler (ticaret şeması)
CREATE INDEX IF NOT EXISTS idx_iade_sirket_tarih ON ticaret.iade(sirket_id, tarih);
CREATE INDEX IF NOT EXISTS idx_iade_kalem_iade ON ticaret.iade_kalem(iade_id);

-- Denetim logu
CREATE INDEX IF NOT EXISTS idx_audit_tarih ON sistem.audit_log(tarih);
CREATE INDEX IF NOT EXISTS idx_audit_islem ON sistem.audit_log(islem);

-- Notlar
CREATE INDEX IF NOT EXISTS idx_not_sirket ON sistem.notlar(sirket_id);

-- Belgeler
CREATE INDEX IF NOT EXISTS idx_belge_entity ON sistem.belge(entity_adi, entity_id);
