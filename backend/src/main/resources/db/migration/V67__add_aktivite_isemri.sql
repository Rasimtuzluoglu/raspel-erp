-- V67__add_aktivite_isemri.sql
-- Aktivite akışı: bildirimlere kullanıcı adı
ALTER TABLE sistem.bildirim ADD COLUMN IF NOT EXISTS kullanici_adi VARCHAR(100);

-- İş emri: siparişten iş emri oluşturma. Gorev artık proje zorunluluğu olmadan (is emri) kullanılabilir.
ALTER TABLE proje.gorev ADD COLUMN IF NOT EXISTS siparis_id BIGINT;
ALTER TABLE proje.gorev ADD COLUMN IF NOT EXISTS personel_id BIGINT;
ALTER TABLE proje.gorev ALTER COLUMN proje_id DROP NOT NULL;
