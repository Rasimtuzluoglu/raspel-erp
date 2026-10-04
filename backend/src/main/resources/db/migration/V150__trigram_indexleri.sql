-- =====================================================================
-- V150: Trigram (pg_trgm) indexleri - arama performansi
--
-- DENETIM BULGUSU (HIGH - performans):
--   Uygulama genelinde `LIKE '%q%'` deseni kullaniliyor:
--     CariHesapRepository.filtreli  -> lower(c.ad) LIKE :q
--                                       lower(c.vergiNumarasi) LIKE :q
--                                       lower(c.telefon) LIKE :q
--     StokRepository.filtreli        -> lower(s.ad) LIKE :q
--                                       lower(s.stokKodu) LIKE :q
--                                       lower(s.barkod) LIKE :q
--     FaturaRepository.ara          -> lower(f.faturaNumarasi) LIKE :q
--                                       lower(c.ad) LIKE :q
--
--   Bu desen NON-SARGABLE (sargable degil): kolonun basindaki sabit
--   olmadigi icin standart B-tree index KULLANILAMAZ. PostgreSQL her
--   aramada tablonun tamamini seq scan ile tarar. Cari/stok listesi
--   ekranlari en sik acilan ekranlardir ve sirket buyudukce bu
--   sorgular tabloyu tamamen tarayarak yavasliyor (kullanici yazarken
--   her tuşta tam tarama).
--
-- COZUM:
--   pg_trgm uzantisi + GIN trigram indexi. Trigram indexleri tam olarak
--   `%q%` desenini hizlandirmak icin tasarlanmistir. `lower(kolon)`
--   uzerine kurulur cunku sorgular `lower(...)` ile karsilastiriyor.
--
-- ETKI:
--   - Arama sorgulari index-only/tanimlayici taramasina duser.
--   - Yazma maliyeti birkac mikrosaniye artar (GIN bakimi).
--
-- GUVENLIK:
--   - Idempotent: IF NOT EXISTS.
--   - GIN indexleri yazma sirasinda tabloyu bloklamaz (INSERT bekler,
--     SELECT engellenmez).
--   - Tablolar kucuk; CONCURRENTLY gerekmiyor ve gerekseydi
--     transaction disinda calistirilmasini gerektirirdi.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- --- Cari hesap: ad, vergi numarasi, telefon (CariHesapRepository.filtreli)
CREATE INDEX IF NOT EXISTS idx_cari_hesap_ad_trgm
    ON cari.cari_hesap USING gin (lower(ad) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_cari_hesap_vergi_numarasi_trgm
    ON cari.cari_hesap USING gin (lower(vergi_numarasi) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_cari_hesap_telefon_trgm
    ON cari.cari_hesap USING gin (lower(telefon) gin_trgm_ops);

-- --- Stok: ad, stok kodu, barkod (StokRepository.filtreli)
CREATE INDEX IF NOT EXISTS idx_stok_ad_trgm
    ON stok.stok USING gin (lower(ad) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_stok_stok_kodu_trgm
    ON stok.stok USING gin (lower(stok_kodu) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_stok_barkod_trgm
    ON stok.stok USING gin (lower(barkod) gin_trgm_ops);

-- --- Fatura: fatura numarasi (FaturaRepository.ara)
CREATE INDEX IF NOT EXISTS idx_fatura_fatura_numarasi_trgm
    ON fatura.fatura USING gin (lower(fatura_numarasi) gin_trgm_ops);

-- --- Personel: ad, TC kimlik, telefon
CREATE INDEX IF NOT EXISTS idx_personel_ad_trgm
    ON personel.personel USING gin (lower(ad) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_personel_tc_kimlik_trgm
    ON personel.personel USING gin (lower(tc_kimlik) gin_trgm_ops);

-- --- Siparis: siparis numarasi
CREATE INDEX IF NOT EXISTS idx_siparis_siparis_no_trgm
    ON siparis.siparis USING gin (lower(siparis_no) gin_trgm_ops);

-- --- Depo / Sube / Hesap plani: kucuk tablolar, ad ile arama
CREATE INDEX IF NOT EXISTS idx_depo_ad_trgm
    ON sube.depo USING gin (lower(ad) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_sube_ad_trgm
    ON sube.sube USING gin (lower(ad) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_hesap_plani_kod_trgm
    ON muhasebe.hesap_plani USING gin (lower(kod) gin_trgm_ops);