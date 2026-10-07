-- =====================================================================
-- V158: Açılış fişi / devir kaydı bayrağı (Faz 2.8)
--
-- Cariye devir (açılış) bakiyesi eklenirken oluşturulan hareketi diğerlerinden
-- ayırt etmek için `acilis` bayrağı. Bakiye etkisi BORC/TAHSILAT türüyle yürür;
-- bu bayrak yalnızca raporlama/izleme amaçlıdır.
--
-- Idempotent.
-- =====================================================================

ALTER TABLE cari.hareket
    ADD COLUMN IF NOT EXISTS acilis boolean NOT NULL DEFAULT false;
