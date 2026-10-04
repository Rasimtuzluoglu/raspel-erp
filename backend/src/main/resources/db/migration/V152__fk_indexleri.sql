-- =====================================================================
-- V152: Foreign key (FK) indexleri
--
-- DENETIM BULGUSU (MEDIUM - performans):
--   PostgreSQL bir FK kolonunda index bulamazsa, parent tabloya silme
--   (DELETE) veya anahtar guncellemesi (UPDATE) sirasinda o tabloyu
--   TAMAMEN tarar (seq scan) ve "referential integrity check" yapar.
--   Kirik (broken link) tespiti de ayni sekilde tam tarama ile calisir.
--
--   Ayrica JOIN kullanan her sorgu bu kolonlarda indeks bulamaz;
--   en sik kullanilan filtreler `sirket_id = ?` ve `personel_id = ?`
--   uzerinden kurulur ve bunlar indekssiz.
--
--   Bu migration, FK'si olan ve ilk kolonunda index BULUNMAYAN
--   kolonlara index ekler. Canli DB'de sorgulayarak tespit edildi:
--     muhasebe.kasa(sirket_id), muhasebe.banka(sirket_id),
--     sistem.gelir_gider_kategori(sirket_id), sistem.rol_yetki(yetki_id),
--     ik.maas_bordro(personel_id), ik.vardiya(personel_id),
--     finans.taksit(cari_id, fatura_id),
--     finans.banka_hareketi(eslesen_fatura_id), fatura.fatura(kasa_id),
--     stok.stok(tedarikci_id, varsayilan_depo_id), cari.cari_fiyat(stok_id),
--     ticaret.fiyat_listesi(stok_id), envanter.stok_seri(stok_hareket_id),
--     muhasebe.hesap_plani(ust_id)
--
-- ETKI:
--   - FK ihlali kontrolu seq scan yerine index-only taramasi yapar.
--   - sirket_id / personel_id filtreli JOIN ve listeleme sorgulari
--     indeks kullanir.
--
-- GUVENLIK:
--   - Idempotent (IF NOT EXISTS).
--   - Salt index; mevcut veriye dokunmaz.
--   - CONCURRENTLY KULLANILMAZ: indeksler kisa tablolarda saniyeler
--     icinde kurulur. CONCURRENTLY transaction disinda calismayi
--     gerektirdigi icin Flyway executeInTransaction ayarini tum dosya
--     icin degistirmeyi gerektirirdi; buna gerek yoktur.
-- =====================================================================

-- --- Tenant izolasyon filtresi: kasa / banka / gelir-gider kategorisi
--     (her istek sirket_id = ? ile baslar)
CREATE INDEX IF NOT EXISTS idx_kasa_sirket ON muhasebe.kasa (sirket_id);
CREATE INDEX IF NOT EXISTS idx_banka_sirket ON muhasebe.banka (sirket_id);
CREATE INDEX IF NOT EXISTS idx_gelir_gider_kategori_sirket
    ON sistem.gelir_gider_kategori (sirket_id);

-- --- Rol -> yetki: yetki silindiginde kopuk satir kontrolu
CREATE INDEX IF NOT EXISTS idx_rol_yetki_yetki_id ON sistem.rol_yetki (yetki_id);

-- --- Personel: bordro ve vardiya listeleri personel_id ile filtrelenir
CREATE INDEX IF NOT EXISTS idx_maas_bordro_personel_id ON ik.maas_bordro (personel_id);
CREATE INDEX IF NOT EXISTS idx_vardiya_personel_id ON ik.vardiya (personel_id);

-- --- Taksit: cari ve fatura bazli vade takibi
CREATE INDEX IF NOT EXISTS idx_taksit_cari_id ON finans.taksit (cari_id);
CREATE INDEX IF NOT EXISTS idx_taksit_fatura_id ON finans.taksit (fatura_id);

-- --- Banka hareketi eslesen fatura (mutabakat sorgulari)
CREATE INDEX IF NOT EXISTS idx_banka_hareketi_eslesen_fatura_id
    ON finans.banka_hareketi (eslesen_fatura_id);

-- --- Fatura: kasa bazli raporlama (kasa kirilimi)
CREATE INDEX IF NOT EXISTS idx_fatura_kasa_id ON fatura.fatura (kasa_id);

-- --- Stok: tedarikci ve varsayilan depo filtreleri
CREATE INDEX IF NOT EXISTS idx_stok_tedarikci_id ON stok.stok (tedarikci_id);
CREATE INDEX IF NOT EXISTS idx_stok_varsayilan_depo_id ON stok.stok (varsayilan_depo_id);

-- --- Cari fiyat / fiyat listesi: stok bazli fiyat sorgulari
CREATE INDEX IF NOT EXISTS idx_cari_fiyat_stok_id ON cari.cari_fiyat (stok_id);
CREATE INDEX IF NOT EXISTS idx_fiyat_listesi_stok_id ON ticaret.fiyat_listesi (stok_id);

-- --- Stok seri: seri hareketine bagli sorgular
CREATE INDEX IF NOT EXISTS idx_stok_seri_stok_hareket_id
    ON envanter.stok_seri (stok_hareket_id);

-- --- Hesap plani hiyerarsisi: ust hesaptan alt hesaplara (muhasebe agaci)
CREATE INDEX IF NOT EXISTS idx_hesap_plani_ust_id ON muhasebe.hesap_plani (ust_id);