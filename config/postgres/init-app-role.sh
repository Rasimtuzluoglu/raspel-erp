#!/bin/sh
# Least-privilege uygulama rolu. Postgres ilk kez baslatilirken (initdb) calisir.
# Superuser (POSTGRES_USER) yerine uygulama bu rolle baglanir. Sifre ortam
# degiskenlerinden gelir; hicbir secret dosyada tutulmaz.
set -e

APP_USER="${APP_DB_USER:-raspelerp_app}"
APP_PASSWORD="${APP_DB_PASSWORD:-}"
DB_NAME="${POSTGRES_DB:-raspelerp}"

if [ -z "$APP_PASSWORD" ]; then
  echo "APP_DB_PASSWORD tanimli degil; least-privilege rol olusturulmadi."
  exit 0
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$DB_NAME" <<-EOSQL
    DO \$do\$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$APP_USER') THEN
            EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT', '$APP_USER', '$APP_PASSWORD');
        ELSE
            EXECUTE format('ALTER ROLE %I WITH LOGIN PASSWORD %L NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT', '$APP_USER', '$APP_PASSWORD');
        END IF;
    END
    \$do\$;

    GRANT CONNECT ON DATABASE $DB_NAME TO $APP_USER;
    GRANT USAGE ON SCHEMA public TO $APP_USER;
EOSQL

# Sema bazli yetkiler (tum semalalar icin; tablolar Flyway ile olusturulduktan
# sonra da gecerli olsun diye varsayilan yetkiler tanimlanir).
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$DB_NAME" <<-EOSQL
    DO \$do\$
    DECLARE
        s text;
    BEGIN
        FOR s IN SELECT nspname FROM pg_namespace
                 WHERE nspname NOT IN ('pg_catalog','information_schema','pg_toast')
        LOOP
            EXECUTE format('GRANT USAGE ON SCHEMA %I TO $APP_USER', s);
            EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA %I TO $APP_USER', s);
            EXECUTE format('GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA %I TO $APP_USER', s);
            EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO $APP_USER', s);
            EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT USAGE, SELECT ON SEQUENCES TO $APP_USER', s);
        END LOOP;
    END
    \$do\$;
EOSQL

echo "Least-privilege rol hazir: $APP_USER"
