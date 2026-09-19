-- V109__teslimat_teslim_imza.sql
-- Kagit kullanmadan dijital teslimat: teslim alan kisi, dijital imza (PNG),
-- teslim notu, GPS konumu ve teslim eden (sofor) adi teslimat kaydinda tutulur.
-- Boylece sofor araçta matbu teslimat fisi tasimak zorunda kalmaz; teslim
-- alan kisi ekranda imzalar ve imza PNG olarak saklanir.
ALTER TABLE ticaret.teslimat ADD COLUMN IF NOT EXISTS teslim_alan_ad VARCHAR(255);
ALTER TABLE ticaret.teslimat ADD COLUMN IF NOT EXISTS teslim_imza_url TEXT;
ALTER TABLE ticaret.teslimat ADD COLUMN IF NOT EXISTS teslim_notu TEXT;
ALTER TABLE ticaret.teslimat ADD COLUMN IF NOT EXISTS teslim_konum VARCHAR(120);
ALTER TABLE ticaret.teslimat ADD COLUMN IF NOT EXISTS teslim_eden_ad VARCHAR(255);
