-- =====================================================================
-- V155: Cari hesaplara kalıcı etiket alanı (Faz 2.4)
--
-- DENETIM BULGUSU:
--   Segmentasyon yalnızca anlık hesaplanıyordu (Gorunum360Service); kullanıcının
--   cariye elle etiket ataması (ör. "vip", "bayilik", "riskli") mümkün değildi.
--
-- COZUM:
--   `cari.cari_hesap.etiketler` kolonu: virgülle ayrık etiketler. AdresDefteri
--   ile aynı desen.
--
-- Idempotent: IF NOT EXISTS.
-- =====================================================================

ALTER TABLE cari.cari_hesap
    ADD COLUMN IF NOT EXISTS etiketler varchar(500);

-- Etiket araması için trigram/pg_trgm kuruluysa GIN index; değilse atlanır.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE schemaname = 'cari' AND indexname = 'idx_cari_hesap_etiketler_trgm'
        ) THEN
            CREATE INDEX idx_cari_hesap_etiketler_trgm
                ON cari.cari_hesap USING gin (etiketler gin_trgm_ops);
            RAISE NOTICE 'V155: cari etiketleri için trigram index oluşturuldu';
        END IF;
    ELSE
        RAISE NOTICE 'V155: pg_trgm yok; etiket index atlandı';
    END IF;
END $$;
