-- V30: Eksik performans index'leri
-- Multi-tenant filtreleme ve iliski bazli sorgulamalar icin

-- Hareketler: tenant filtreleme
CREATE INDEX IF NOT EXISTS idx_hareket_sirket ON cari.hareket(sirket_id);

-- Cek/Senet: cari + durum filtreleme
CREATE INDEX IF NOT EXISTS idx_cek_senet_cari ON muhasebe.cek_senet(cari_hesap_id);
CREATE INDEX IF NOT EXISTS idx_cek_senet_durum ON muhasebe.cek_senet(durum);

-- Kasa hareket: kasa bazli filtreleme
CREATE INDEX IF NOT EXISTS idx_kasa_hareket_kasa ON muhasebe.kasa_hareket(kasa_id);

-- Personel izin: personel bazli filtreleme
CREATE INDEX IF NOT EXISTS idx_personel_izin_personel ON personel.personel_izin(personel_id);

-- Proje gorev: proje bazli filtreleme
CREATE INDEX IF NOT EXISTS idx_gorev_proje ON proje.gorev(proje_id);

-- Muhasebe fis: sirket + tarih filtreleme
CREATE INDEX IF NOT EXISTS idx_muhasebe_fis_sirket ON muhasebe.muhasebe_fisi(sirket_id);

-- E-fatura: sirket bazli
CREATE INDEX IF NOT EXISTS idx_e_fatura_sirket ON fatura.e_fatura(sirket_id);

-- Doviz kuru: kod + tarih (zaten unique constraint var, ek index)
CREATE INDEX IF NOT EXISTS idx_doviz_kuru_kod ON finans.doviz_kuru(doviz_kodu, tarih);

-- Butce: sirket bazli
CREATE INDEX IF NOT EXISTS idx_butce_sirket ON finans.butce(sirket_id);

-- Masraf: sirket bazli
CREATE INDEX IF NOT EXISTS idx_masraf_sirket ON finans.masraf(sirket_id);
