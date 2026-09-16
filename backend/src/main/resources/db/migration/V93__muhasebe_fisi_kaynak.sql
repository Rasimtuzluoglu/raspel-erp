-- V93__muhasebe_fisi_kaynak.sql
-- Otomatik yevmiye fisi kaynagini izlemek ve ayni kaynak icin mukerrer fisi
-- onlemek amaciyla kaynak tipi/id eklenir. Aktif (IPTAL olmayan) fisler icin
-- (sirket_id, kaynak_tip, kaynak_id) benzersizdir; IPTAL edilenler audit icin kalir.
ALTER TABLE muhasebe.muhasebe_fisi ADD COLUMN IF NOT EXISTS kaynak_tip VARCHAR(30);
ALTER TABLE muhasebe.muhasebe_fisi ADD COLUMN IF NOT EXISTS kaynak_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_muhasebe_fisi_kaynak
    ON muhasebe.muhasebe_fisi (sirket_id, kaynak_tip, kaynak_id)
    WHERE kaynak_tip IS NOT NULL AND durum <> 'IPTAL';
