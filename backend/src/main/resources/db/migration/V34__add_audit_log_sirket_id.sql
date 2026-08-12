-- V34__add_audit_log_sirket_id.sql
ALTER TABLE sistem.audit_log ADD COLUMN IF NOT EXISTS sirket_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_audit_log_sirket_id ON sistem.audit_log(sirket_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_kullanici_id ON sistem.audit_log(kullanici_id);
