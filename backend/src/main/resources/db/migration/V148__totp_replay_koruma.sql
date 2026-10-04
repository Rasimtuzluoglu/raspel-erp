-- TOTP replay koruması: son başarılı doğrulanan zaman adımı (RFC 6238 counter).
--
-- Gerekçe: doğrulama penceresi ±1 adım (±30 sn) olduğu için geçerli bir 6 haneli
-- kod üç kez kabul ediliyordu. Ekrandan görülen bir kod (omuz sörfü, ekran
-- görüntüsü, paylaşılan mesaj) tekrar kullanılabiliyordu.
--
-- Güvenli: yalnızca yeni kolon, nullable, mevcut satırlar NULL kalır ve NULL
-- "hiç kod kullanılmadı" olarak yorumlanır. Geri alınabilir (DROP COLUMN).

ALTER TABLE sistem.kullanici ADD COLUMN IF NOT EXISTS two_factor_last_counter BIGINT;