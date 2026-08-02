-- =============================================================
-- V29: 2FA kolonları + DovizKuru efektif kolonları (prod uyumu)
-- =============================================================

-- 1. Kullanıcı 2FA alanları
ALTER TABLE sistem.kullanici ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE sistem.kullanici ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(100);

-- 2. DovizKuru efektif kur kolonları (entity ile uyum)
ALTER TABLE finans.doviz_kuru ADD COLUMN IF NOT EXISTS efektif_alis NUMERIC(19,4);
ALTER TABLE finans.doviz_kuru ADD COLUMN IF NOT EXISTS efektif_satis NUMERIC(19,4);
