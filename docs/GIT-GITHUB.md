# Espirix — Passo a passo Git e GitHub (Windows + IntelliJ)

> Antes: preserve o backup da v0.3. Não publique `.env`, bancos H2/PostgreSQL, relatórios reais ou screenshots com senhas.

## A. Commit da v0.3 atual

No terminal PowerShell do IntelliJ, na pasta `frontend`, primeiro volte à raiz:

```powershell
cd ..
git status
git branch --show-current
```

Se você estiver na `feat/espirix-v03` com alterações da v0.3 e ainda não tiver commitado:

```powershell
git add README.md backend frontend .github docs examples scripts docker-compose.yml docker-compose.public.yml Caddyfile .env.example .gitignore GUIA-V03.md
git diff --cached --name-only
git diff --cached --check
git commit -m "feat: autenticação e isolamento de empresas v0.3"
```

Remova da área de staging qualquer segredo ou dado real antes do commit: `git restore --staged CAMINHO`. Não use `git add -f`.

## B. Criar branch da v0.4 e aplicar o patch

```powershell
git switch -c feat/espirix-v04-cloud
```

Extraia `Espirix-v0.4-PATCH.zip` sobre a **raiz do repositório**. Não substitua a pasta `data/`. Execute testes pelo painel Maven (Lifecycle > verify) e no terminal:

```powershell
cd frontend
npm.cmd install
npm.cmd run build
cd ..
git status --short
git diff --check
```

Depois:

```powershell
git add .github .gitignore .env.example backend frontend infra scripts docker-compose.yml docker-compose.public.yml Caddyfile README.md GUIA-V04.md docs
git diff --cached --name-only
git diff --cached --check
git commit -m "feat: preparar Espirix v0.4 cloud, Flyway e backups"
git push -u origin feat/espirix-v04-cloud
```

No GitHub, abra **Pull Request** de `feat/espirix-v04-cloud` para `main`. Aguarde CI verde, revise arquivos/diff e só então faça merge. Se o repositório for pessoal e você preferir merge local:

```powershell
git switch main
git pull --ff-only origin main
git merge --no-ff feat/espirix-v04-cloud
git push origin main
```

Não faça os dois tipos de merge para a mesma alteração. Evite `git push --force` e não faça merge com testes falhando.

## C. Se já tiver commitado a v0.3 em outro branch

Use `git status` e `git log --oneline -5` para verificar; não crie commit duplicado. Aplique apenas v0.4 em branch separada.

## D. Atualizar o servidor depois de publicar

No servidor Linux (não no Windows local):

```sh
cd /srv/espirix
git fetch --all --prune
git pull --ff-only origin main
bash scripts/backup-postgres.sh
bash scripts/deploy-cloud.sh
```

Nunca comite `backups/`, `.env`, `data/`, PDFs reais, bancos ou arquivos de clientes. Revise o histórico antigo do GitHub se eles já tiverem sido incluídos.
