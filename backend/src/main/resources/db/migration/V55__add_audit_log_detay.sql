-- V55__add_audit_log_detay.sql
-- Denetim loglarına değişiklik detayı (önce/sonra değer) alanı ekler.
ALTER TABLE sistem.audit_log ADD COLUMN IF NOT EXISTS detay TEXT;
