-- Faz 4 (genisletme): sirket_id -> sistem.sirket tenant FK ve NOT NULL kapsamini
-- tum sirket_id tasiyan tablolara yay. Idempotent (dinamik, mevcut FK'lari atlar).
--
-- NOT NULL istisnalari (semantik olarak null olabilir / veri):
--   sistem.kullanici      -> super-admin sirkete bagli olmayabilir
--   sistem.audit_log      -> oturum oncesi sistem olaylari
--   sistem.hata_log       -> oturum oncesi hatalar
--   satinalma.satinalma_talep -> AI uretimli, 1 kayit sirketsiz (backfill edilmedi)

-- 1) Tenant FK: sirket_id kolonu olup FK'si olmayan tum tablolara ekle + dogrula.
DO $$
DECLARE
    r record;
    fk_ad text;
BEGIN
    FOR r IN
        SELECT c.table_schema, c.table_name
        FROM information_schema.columns c
        WHERE c.column_name = 'sirket_id'
          AND c.table_schema NOT IN ('pg_catalog', 'information_schema')
          AND NOT EXISTS (
              SELECT 1
              FROM pg_constraint pc
              JOIN pg_class rel ON rel.oid = pc.conrelid
              JOIN pg_namespace ns ON ns.oid = rel.relnamespace
              JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (pc.conkey)
              WHERE pc.contype = 'f'
                AND ns.nspname = c.table_schema
                AND rel.relname = c.table_name
                AND att.attname = 'sirket_id'
          )
    LOOP
        fk_ad := 'fk_' || r.table_name || '_sirket';
        EXECUTE format(
            'ALTER TABLE %I.%I ADD CONSTRAINT %I FOREIGN KEY (sirket_id) REFERENCES sistem.sirket(id) NOT VALID',
            r.table_schema, r.table_name, fk_ad);
        EXECUTE format('ALTER TABLE %I.%I VALIDATE CONSTRAINT %I',
                       r.table_schema, r.table_name, fk_ad);
    END LOOP;
END $$;

-- 2) sirket_id NOT NULL: null satir bulunmayan ve istisna olmayan tablolar.
DO $$
DECLARE
    r record;
    null_sayi bigint;
BEGIN
    FOR r IN
        SELECT c.table_schema, c.table_name
        FROM information_schema.columns c
        WHERE c.column_name = 'sirket_id'
          AND c.is_nullable = 'YES'
          AND c.table_schema NOT IN ('pg_catalog', 'information_schema')
          AND NOT (c.table_schema = 'sistem' AND c.table_name IN ('kullanici', 'audit_log', 'hata_log'))
          AND NOT (c.table_schema = 'satinalma' AND c.table_name = 'satinalma_talep')
    LOOP
        EXECUTE format('SELECT count(*) FROM %I.%I WHERE sirket_id IS NULL',
                       r.table_schema, r.table_name) INTO null_sayi;
        IF null_sayi = 0 THEN
            EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN sirket_id SET NOT NULL',
                           r.table_schema, r.table_name);
        END IF;
    END LOOP;
END $$;
