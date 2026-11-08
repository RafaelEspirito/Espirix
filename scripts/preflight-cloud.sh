#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")/.."
test -f .env || { echo 'ERRO: falta .env. Copie .env.example e configure sem commitar.' >&2; exit 1; }
chmod 600 .env
set -a
# shellcheck disable=SC1091
. ./.env
set +a
: "${DB_ADMIN_PASSWORD:?DB_ADMIN_PASSWORD ausente}"
: "${DB_PASSWORD:?DB_PASSWORD ausente}"
: "${ESPIRIX_MASTER_EMAIL:?ESPIRIX_MASTER_EMAIL ausente}"
: "${ESPIRIX_MASTER_PASSWORD:?ESPIRIX_MASTER_PASSWORD ausente}"
: "${ESPIRIX_DOMAIN:?ESPIRIX_DOMAIN ausente}"
: "${CORS_ORIGINS:?CORS_ORIGINS ausente}"
if [ ${#DB_ADMIN_PASSWORD} -lt 20 ] || [ ${#DB_PASSWORD} -lt 20 ] || [ ${#ESPIRIX_MASTER_PASSWORD} -lt 16 ]; then
  echo 'ERRO: senhas precisam ser longas (DB >=20, AdminMaster >=16).' >&2; exit 1
fi
if [[ "$ESPIRIX_DOMAIN" == *"seudominio"* ]] || [[ "$CORS_ORIGINS" != "https://$ESPIRIX_DOMAIN" ]]; then
  echo 'ERRO: defina dominio real e CORS_ORIGINS=https://DOMINIO.' >&2; exit 1
fi
if [ "${ESPIRIX_PERMITIR_LIMPEZA_TRABALHO:-false}" != false ]; then
  echo 'ERRO: exclusao total no Trabalho deve ficar BLOQUEADA.' >&2; exit 1
fi
command -v docker >/dev/null || { echo 'ERRO: instale Docker e compose plugin.' >&2; exit 1; }
docker compose -f docker-compose.yml -f docker-compose.public.yml config --quiet
printf 'Preflight basico OK. Isso nao substitui teste de seguranca, DNS, backups ou auditoria.
'
