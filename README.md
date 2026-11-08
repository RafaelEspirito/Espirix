# Espirix v0.4 — Preparação para Cloud

Plataforma de gestão financeira da Espirito Labs: Java 21, Spring Boot 3.5,
React, TypeScript e PostgreSQL. Importação CSV/PDF; dashboard; títulos;
pagamentos parciais; autenticação e isolamento de empresas.

**ESTADO: candidato a implantação de HOMOLOGAÇÃO. NÃO homologado para dados corporativos reais.**
O código foi preparado para execução em containers; a instância pública e os controles
organizacionais precisam ser configurados e testados pelo responsável técnico.

- Instalação guiada: [GUIA-V04.md](GUIA-V04.md)
- Checklist de homologação: [docs/HOMOLOGACAO-CLOUD.md](docs/HOMOLOGACAO-CLOUD.md)
- Procedimento Git/GitHub: [docs/GIT-GITHUB.md](docs/GIT-GITHUB.md)
- **H2 local é preservado**. Docker/PostgreSQL começa vazio; não migra automaticamente.
- Contas `ADMIN_MASTER`, `ADMIN`, `FINANCEIRO` e `CONSULTA`.
- Perfil Demo permanece separado do Trabalho. Limpeza total do Trabalho bloqueada no servidor.
- Banco PostgreSQL: Flyway habilitado em `cloud`, Hibernate em `validate`.

## Execução local (sem Docker)
Java 21 no IntelliJ e `frontend: npm install && npm run dev`. Usa H2 no perfil padrão.

## Testes
`mvn -f backend/pom.xml clean verify` (IntelliJ: Maven > Lifecycle > verify).
`cd frontend && npm install && npm run build`.

## Deploy Linux (apenas homologação)
Servidor Ubuntu com Docker Engine, Compose plugin, DNS e portas 80/443:

```sh
cp .env.example .env
chmod 600 .env
# edite .env com senhas ÚNICAS e domínio verdadeiro
bash scripts/preflight-cloud.sh
bash scripts/deploy-cloud.sh
```

Banco privado sem porta pública. Frontend atrás de Caddy/HTTPS. Backups:
`bash scripts/backup-postgres.sh` e restauração de teste:
`bash scripts/verify-restore.sh backups/espirix-....dump`.

**Segurança pendente antes de dados reais:** revisão de autorização corporativa,
2FA/SSO para administração, análise de vulnerabilidades, backup externo criptografado,
alertas e teste de restauração no servidor, teste real dos dois usuários/empresas,
aceite do responsável pelo tratamento de dados.
