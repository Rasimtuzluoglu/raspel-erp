-- V60__add_teklif_no_unique.sql
-- Teklif numarası için benzersizlik kısıtı (çoklu instance'ta mükerrer numara üretimini DB seviyesinde engeller).
-- Fatura ve sipariş numaralarında zaten unique kısıt mevcuttu.
CREATE UNIQUE INDEX IF NOT EXISTS uq_teklif_teklif_no ON ticaret.teklif(teklif_no);
