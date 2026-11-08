#!/bin/sh
set -eu
: "${ESPIRIX_DB_PASSWORD:?Senha de aplicacao ausente}"
# Executa APENAS em volume PostgreSQL novo; senha nao entra em logs/SQL versionado.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB"   -v app_password="$ESPIRIX_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', 'espirix_app', :'app_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'espirix_app') \gexec
GRANT CONNECT ON DATABASE espirix TO espirix_app;
GRANT USAGE, CREATE ON SCHEMA public TO espirix_app;
SQL
