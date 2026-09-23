-- Faz 4: İyimser kilitleme (optimistic locking).
-- Finans/stok satır ve başlık entity'lerine @Version eklenir; eşzamanlı
-- güncellemelerde kayıp veri (lost update) sessizce oluşmaz, OptimisticLockException
-- ile yüzeye çıkar. Mevcut satırlar 0 ile başlar.

ALTER TABLE muhasebe.kasa_hareket   ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE finans.banka_hareketi   ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cari.hareket            ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ticaret.iade            ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE siparis.siparis         ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE muhasebe.irsaliye       ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE finans.taksit           ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE stok.stok_hareket       ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
