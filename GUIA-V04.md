# Espirix v0.4 — Guia de instalação e publicação

**Status:** pacote para implantação/homologação, não pronto para dados reais por mera instalação.

## 0. O que você tem e o que NÃO é migrado

- O projeto v0.3 local continua usando o **H2** `./data/espirix-v03.mv.db`. A v0.4 não apaga esse banco.
- A implantação Docker utiliza um **PostgreSQL completamente novo**, sem dados e sem usuários de trabalho; somente o AdminMaster é cadastrado pelo bootstrap.
- O CSV de demonstração deve ser reimportado no Demo; contas de Rafael e Micael devem ser criadas no ambiente Trabalho outra vez.
- Guarde backup offline antes de aplicar o PATCH. Atenção: backup do projeto no Google Drive pode conter dados sensíveis e segredos; mantenha autorização e criptografia.

## 1. Atualizar o projeto no Windows (IntelliJ)

1. Abra seu projeto em `D:\Projeto Chat\omie-financeiro-etapa1\omie-financeiro-etapa1`.
2. Mantenha sua cópia de segurança fechada e intacta.
3. Extraia **Espirix-v0.4-PATCH.zip** e copie o conteúdo das pastas para a **raiz** do projeto, sobrescrevendo apenas os arquivos com mesmo nome. NÃO apague `backend`, `frontend` ou `data`.
4. Não cole o conteúdo do ZIP como uma pasta aninhada (`Espirix-v0.4-PATCH/Espirix/...`): as pastas `backend` e `frontend` devem ficar no mesmo nível do seu `.git`.
5. No IntelliJ, em Maven > Lifecycle, rode `clean` e `verify` do módulo `espirix-backend`.
6. No terminal dentro de `frontend`: `npm.cmd install`, depois `npm.cmd run build`.
7. Execute `EspirixApplication` normalmente **sem perfil ativo** para manter seu H2 atual; não configure perfil `cloud` para o desenvolvimento local.
8. Teste login, Demo/Trabalho, títulos, reimportação e pagamentos.

**Se Maven falhar, NÃO suba código sem corrigir.** O workflow no GitHub executará build Java e React novamente.

## 2. Primeiro deploy para homologação (servidor Linux)

Você precisará de:

- VPS ou servidor Linux sob controle da empresa, Docker Engine + plugin Compose, RAM/disco suficientes e acesso SSH.
- Domínio/subdomínio próprio sob sua administração, DNS tipo A apontando para IP público do servidor.
- Portas 80 e 443 liberadas na firewall; **não abra 5432, 8080 ou 8085**.
- Autorização formal para hospedar informações corporativas fora do OMIE e definição de responsável por LGPD, custos e suporte.
- MFA externo (ex.: gateway de identidade/SSO) ou autenticação multifator implantada e testada antes de liberar dados reais.
- A tela Preferências > Segurança da conta permite trocar sua senha; o .env só define senha inicial ao criar o master pela primeira vez.

1. Confirme no GitHub que o código está na branch/release desejada, sem `.env`/dados.
2. Pelo SSH no servidor, clone o repositório: `git clone https://github.com/RafaelEspirito/Espirix.git && cd Espirix` (ajuste conforme a URL real).
3. `cp .env.example .env && chmod 600 .env`
4. Edite `.env` com duas senhas de banco **diferentes**, e-mail e senha de AdminMaster, `ESPIRIX_DOMAIN` e `CORS_ORIGINS=https://ESSE-DOMINIO`.
5. NÃO use uma senha que já apareceu em capturas ou no chat.
6. `bash scripts/preflight-cloud.sh`
7. `bash scripts/deploy-cloud.sh`
8. `docker compose -f docker-compose.yml -f docker-compose.public.yml ps` e `docker compose -f docker-compose.yml -f docker-compose.public.yml logs --tail=100 backend caddy`
9. Acesse `https://SEU_DOMINIO/actuator/health` (esperado JSON `UP`), teste login/CSRF, troca de ambiente, usuário financeiro e logout. Não suba relatórios reais aqui!

**ATENÇÃO:** certificado TLS automático do Caddy exige DNS público correto e servidor acessível em 80/443. Nesta versão, não há domínio contratado, servidor provisionado ou DNS configurado por você. O ZIP não cria isso sozinho.

## 3. Homologação com arquivos fictícios

- AdminMaster acessa Demo e Trabalho; Rafael e Micael são contas FINANCEIRO restritas à mesma empresa.
- Importe os arquivos CSV fictícios nos ambientes correspondentes e teste reimportações.
- Abra dois navegadores/sessões simultâneos; faça pagamento parcial com uma conta e confira com a outra.
- Tente acessar `/api/auth/empresas` com o usuário Financeiro: deve retornar 403.
- Tente mudar o `empresaId` de um título entre empresas: deve responder 404/403, nunca revelar dados.
- Confirme que a exclusão total do ambiente Trabalho continua bloqueada no servidor.
- Reinicie containers sem remover volumes e confira persistência dos dados.
- Execute backup e restauração de teste. Confira `docs/HOMOLOGACAO-CLOUD.md`.

## 4. Backups (não dependem do computador do desenvolvedor)

Na pasta do projeto no Linux:

```sh
bash scripts/backup-postgres.sh
bash scripts/verify-restore.sh backups/espirix-AAAA...dump
```

O script cria um dump **local** em `backups/` e valida que ele pode ser lido. O segundo restaura em banco temporário de teste e o remove. Isso não equivale a disaster recovery: configure cópia externa criptografada, retenção, alertas de falha e um procedimento de recuperação documentado antes de dados reais.

Exemplo de agendamento diário às 02h (adapte fuso, diretório, usuário e logs):

```cron
0 2 * * * cd /srv/espirix && /usr/bin/bash scripts/backup-postgres.sh >> /var/log/espirix-backup.log 2>&1
```

Cron é apenas sugestão. Sem configurar no servidor, nada roda sozinho. Preserve pelo menos backups criptografados fora do servidor e monitore a execução.

## 5. Como atualizar depois do deploy

No servidor, após CI verde e backup válido:

```sh
git fetch --all --prune
git pull --ff-only origin main
bash scripts/backup-postgres.sh
bash scripts/deploy-cloud.sh
```

**Nunca execute** `docker compose down -v`, `docker volume rm`, `flyway clean` ou SQL `DROP` no banco de trabalho. Esses comandos podem apagar dados.

## 6. Rollback

Não existe rollback automático de migrações. Guarde um dump verificado do PostgreSQL antes de atualizar; reverta código para tag/commit anterior **somente se o esquema for compatível**. Caso contrário, restauração isolada e procedimento formal.

## 7. Limites técnicos

- `Spring Security` usa sessões no servidor. Um restart do backend pode exigir login novamente; dados permanecem no PostgreSQL.
- A autenticação atual **não tem MFA nativo**, e não foi executado pentest independente. NÃO afirmar que a versão já protege dados reais na internet.
- Scripts Docker e schema Flyway precisam da primeira execução e validação em PostgreSQL real. Não foram executados nesta sessão.
- Ainda faltam métricas/alertas completos, gestão de incidentes, contratos e avaliação de proteção de dados.
