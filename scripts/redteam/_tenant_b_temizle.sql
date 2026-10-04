DO $$
DECLARE
    r record;
    silinen int;
    toplam int := 0;
BEGIN
    FOR r IN
        SELECT n.nspname AS schema_ad, c.relname AS tablo_ad
        FROM pg_constraint con
        JOIN pg_class c ON c.oid = con.conrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        JOIN pg_class rc ON rc.oid = con.confrelid
        JOIN pg_namespace rn ON rn.oid = rc.relnamespace
        WHERE con.contype = 'f'
          AND rn.nspname = 'sistem'
          AND rc.relname = 'sirket'
          AND c.relname <> 'sirket'
    LOOP
        EXECUTE format('DELETE FROM %I.%I WHERE sirket_id = $1', r.schema_ad, r.tablo_ad) USING 99;
        GET DIAGNOSTICS silinen = ROW_COUNT;
        IF silinen > 0 THEN
            toplam := toplam + silinen;
            RAISE NOTICE 'Tenant B silindi: %.% (%) satir', r.schema_ad, r.tablo_ad, silinen;
        END IF;
    END LOOP;
    RAISE NOTICE '--- Toplam % satir silindi', toplam;
END $$;