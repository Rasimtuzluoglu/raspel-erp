-- V87__add_backup_ayar_table.sql
-- Bulut yedekleme yapılandırmasının kalıcı saklandığı tek satırlık tablo.
-- BackupService daha önce bu ayarı yalnızca in-memory map'te tutuyordu; uygulama
-- yeniden başladığında kayboluyordu (autoSync/encryptionEnabled dahil).
-- id = 1 sabit tek satır (upsert ile güncellenir).
CREATE TABLE IF NOT EXISTS sistem.backup_ayar (
    id INT PRIMARY KEY,
    provider VARCHAR(50) NOT NULL DEFAULT 'MINIO',
    bucket VARCHAR(255) NOT NULL DEFAULT '',
    region VARCHAR(100) NOT NULL DEFAULT '',
    auto_sync BOOLEAN NOT NULL DEFAULT FALSE,
    encryption_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    last_sync_time TIMESTAMP NULL,
    guncelleme_tarihi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);