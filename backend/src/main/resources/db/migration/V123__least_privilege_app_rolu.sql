-- Faz G1: Uygulama icin least-privilege DB rolu.
-- Süper kullanici (postgres) yerine, yalnizca DML yetkili bir uygulama rolu
-- tanimlanir. Rol adi ve sifresi ortam degiskeni ile verilir; migration yalnizca
-- rol mevcut degilse olusturur ve grant'leri uygular (idempotent).
--
-- Uygulama bu migrasyonu superuser ile calistirir (Flyway), sonrasi app rolu ile
-- baglanir. Sifre ortam degiskeninden okunur: APP_DB_PASSWORD.

DO $mig$
DECLARE
    app_rol text := 'raspelerp_app';
    app_sifre text := '${app-db-password}';
BEGIN
    IF app_sifre IS NULL OR app_sifre = '' OR app_sifre = '${app-db-password}' THEN
        RAISE NOTICE 'APP_DB_PASSWORD tanimli degil; least-privilege rol olusturulmadi (dev/test uyumlulugu).';
        RETURN;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = app_rol) THEN
        EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT', app_rol, app_sifre);
    ELSE
        EXECUTE format('ALTER ROLE %I WITH LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT', app_rol, app_sifre);
    END IF;
END $mig$;

-- Semalari kullanma yetkisi
DO $mig2$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'raspelerp_app') THEN
        GRANT USAGE ON SCHEMA sistem, cari, stok, fatura, siparis, satinalma, muhasebe,
            personel, proje, sube, finans, ticaret, envanter, ik, maliyet, public TO raspelerp_app;

        -- Mevcut nesneler
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA
            sistem, cari, stok, fatura, siparis, satinalma, muhasebe, personel, proje,
            sube, finans, ticaret, envanter, ik, maliyet, public TO raspelerp_app;
        GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA
            sistem, cari, stok, fatura, siparis, satinalma, muhasebe, personel, proje,
            sube, finans, ticaret, envanter, ik, maliyet, public TO raspelerp_app;

        -- Gelecekte olusturulacak nesneler icin varsayilan yetkiler
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA sistem GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA cari GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA stok GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA fatura GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA siparis GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA satinalma GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA muhasebe GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA personel GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA proje GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA sube GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA finans GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA ticaret GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA envanter GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA ik GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA maliyet GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO raspelerp_app';

        -- Flyway gecmis tablosunu okuyabilmeli (spring.flyway validate)
        GRANT SELECT ON TABLE public.flyway_schema_history TO raspelerp_app;
    END IF;
END $mig2$;
