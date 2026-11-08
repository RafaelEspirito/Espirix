#!/usr/bin/env bash
# Teste REAL de restauração em banco TEMPORARIO, sem substituir espirix.
set -Eeuo pipefail
if [ $# -ne 1 ]; then echo 'Uso: scripts/verify-restore.sh backups/espirix-AAAAMMDD.dump' >&2; exit 1; fi
file="$(realpath "$1")"
test -s "$file" || { echo 'Arquivo nao encontrado ou vazio' >&2; exit 1; }
cd "$(dirname "$0")/.."
project="espirix_restorecheck_$(date +%s)"
DB=(docker compose -f docker-compose.yml -f docker-compose.public.yml exec -T db)
cleanup(){
  "${DB[@]}" sh -c 'export PGPASSWORD="$POSTGRES_PASSWORD"; dropdb -U postgres --if-exists --force "$1"' sh "$project" >/dev/null 2>&1 || true
}
trap cleanup EXIT
"${DB[@]}" sh -c 'export PGPASSWORD="$POSTGRES_PASSWORD"; createdb -U postgres "$1"' sh "$project"
"${DB[@]}" sh -c 'export PGPASSWORD="$POSTGRES_PASSWORD"; exec pg_restore -U postgres -d "$1" --no-owner --no-acl --exit-on-error' sh "$project" < "$file"
"${DB[@]}" sh -c 'export PGPASSWORD="$POSTGRES_PASSWORD"; psql -U postgres -d "$1" -Atc "SELECT COUNT(*) FROM empresas"' sh "$project" | grep -E '^[0-9]+$' >/dev/null
echo "Restauracao validada no banco temporario $project (sera removido)."
