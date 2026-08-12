-- V39__normalize_odeme_durumu.sql
-- HizliSatis'tan gelen Türkçe ödeme durumu etiketlerini enum değerlerine çevirir.
UPDATE fatura.fatura SET odeme_durumu = 'ODENDI' WHERE odeme_durumu IN ('Tamamen Ödendi', 'Tamamen Ödendi ');
UPDATE fatura.fatura SET odeme_durumu = 'KISMI_ODENDI' WHERE odeme_durumu IN ('Kısmi Ödendi', 'Kısmi Ödendi ');
UPDATE fatura.fatura SET odeme_durumu = 'ODENMEDI' WHERE odeme_durumu IN ('Ödenmedi', 'Ödenmedi ');
