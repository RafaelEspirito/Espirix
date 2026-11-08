#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")/.."
bash scripts/preflight-cloud.sh
# Nao efetua docker compose down -v: o volume do banco deve permanecer.
docker compose -f docker-compose.yml -f docker-compose.public.yml up -d --build
printf '
Servicos:
'
docker compose -f docker-compose.yml -f docker-compose.public.yml ps
printf '
Confira HTTPS e acesse https://%s (nao importe dados reais antes da homologacao).
' "${ESPIRIX_DOMAIN:-DOMINIO}"