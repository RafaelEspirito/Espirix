#!/usr/bin/env bash
# Executar no SERVIDOR na pasta do projeto, com docker compose em funcionamento.
set -Eeuo pipefail
cd "$(dirname "$0")/.."
umask 077
mkdir -p backups
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
tmp="backups/espirix-${stamp}.dump.part"
out="backups/espirix-${stamp}.dump"
trap 'rm -f "$tmp"' EXIT
# Pg_dump dentro do contêiner usa credenciais privadas; dump fica FORA do Git.
docker compose -f docker-compose.yml -f docker-compose.public.yml exec -T db sh -c   'export PGPASSWORD="$POSTGRES_PASSWORD"; exec pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -Z6' > "$tmp"
test -s "$tmp"
docker compose -f docker-compose.yml -f docker-compose.public.yml exec -T db pg_restore -l < "$tmp" >/dev/null
mv "$tmp" "$out"
chmod 600 "$out"
trap - EXIT
echo "Backup criado: $out"
echo 'IMPORTANTE: configure uma COPIA EXTERNA CRIPTOGRAFADA e teste restauracao periodicamente.'
