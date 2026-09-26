-- Kişisel bildirimlerin yalnızca ilgili kullanıcıya gösterilmesi için kullanıcı kimliği.
-- NULL = şirkete/tüm kullanıcılara ait genel bildirim.
ALTER TABLE sistem.bildirim ADD COLUMN IF NOT EXISTS kullanici_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_bildirim_kullanici ON sistem.bildirim (kullanici_id);
