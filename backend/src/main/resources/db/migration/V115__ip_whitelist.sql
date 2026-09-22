-- IP beyaz listesi: kalici, sirket (tenant) bazli guvenli IP tanimlari.
CREATE TABLE IF NOT EXISTS sistem.ip_whitelist (
    id BIGSERIAL PRIMARY KEY,
    sirket_id BIGINT,
    ip_adresi VARCHAR(100) NOT NULL,
    aciklama VARCHAR(255),
    durum VARCHAR(20) NOT NULL DEFAULT 'AKTIF',
    ekleme_tarihi DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE INDEX IF NOT EXISTS idx_ip_whitelist_sirket ON sistem.ip_whitelist (sirket_id);
