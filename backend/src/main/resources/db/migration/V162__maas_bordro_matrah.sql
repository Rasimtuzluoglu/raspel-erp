-- V162__maas_bordro_matrah.sql
-- Kümülatif gelir vergisi için bordro satırında gelir vergisi matrahı saklanır.
-- Önce toplu üretim her ayı "ilk ay" gibi hesaplıyordu (kumulatif matrah hep 0);
-- yüksek ücretlilerde yıl boyunca eksik vergi kesiliyordu. Matrah saklanınca
-- önceki ayların toplamı doğru hesaplanabilir.

ALTER TABLE ik.maas_bordro
    ADD COLUMN IF NOT EXISTS gelir_vergisi_matrahi NUMERIC(19,2) DEFAULT 0;
