# Espirix v0.3 — autenticação, ambientes separados e limpeza segura

**Espirito Labs · Pacote de evolução / pré-produção.**

> ⚠️ Este é um pacote de código de referência preparado sobre o backend original e o frontend que já havíamos criado. Não houve execução dos testes Maven nem teste end-to-end deste pacote neste ambiente. Não implante com dados empresariais reais antes de executar os testes, revisar as diferenças e autorizar formalmente o tratamento de dados.

## O que está implementado no código

- Tela de login React com autenticação por sessão Spring Security. As senhas são armazenadas como hash BCrypt, não em texto puro.
- Proteção CSRF ativa: `/api/auth/csrf`; o frontend anexa o token aos POSTs.
- Papéis `ADMIN_MASTER`, `ADMIN`, `FINANCEIRO`, `CONSULTA`. Administrador master pode alternar a empresa ativa; as demais contas têm associação fixa a uma empresa.
- Empresas iniciais `demo` (Demonstração) e `trabalho` (Ambiente de trabalho), sem dados preexistentes.
- `empresa_id` em clientes, títulos, pagamentos e lotes de importação, com filtros nas consultas e buscas individuais; chaves únicas incluem empresa para não colidir com a demo.
- Área *Preferências > Gerenciar equipe* para criar acessos no ambiente atual. O AdminMaster pode criar administradores da empresa; o administrador da empresa cria Financeiro/Consulta.
- *Preferências > Zona de perigo*: prévia de contagens, frase explícita, confirmação final e endpoint transacional que exclui dados financeiros **da empresa ativa somente**, na ordem pagamentos → títulos → clientes → importações. Um registro de auditoria da limpeza é preservado.
- A limpeza da `demo` fica habilitada. A limpeza de `trabalho` fica **desabilitada por padrão**, e só pode ser liberada via `ESPIRIX_PERMITIR_LIMPEZA_TRABALHO=true` no servidor.
- Dockerfiles e Compose opcional para hospedagem, com PostgreSQL e Nginx; Caddy opcional para HTTPS com domínio válido.
- Teste de integração adicional `TenantIsolationIntegrationTest` (precisa ser executado e validado localmente).

### Atenção ao significado de “perfil”

Os ambientes são **workspaces de empresa**, não bancos individuais por login. Você, como master, pode alternar entre Demo e Trabalho. Seu colega, como Financeiro associado a Trabalho, só vê os dados de Trabalho. Excluir os dados de Trabalho afetará todos os usuários desse ambiente. Excluir Demo não deverá afetar Trabalho.

### O que o botão de limpar faz

Ele **não** exclui apenas linhas do histórico de arquivos. Apaga integralmente todos os **dados financeiros do workspace ativo**, incluindo pagamentos manuais realizados em títulos importados, títulos, clientes e importações. As contas de usuário e o registro da operação de limpeza são preservados. É irreversível sem backup.

## 1. Fazer backup e criar uma branch

Na raiz do repositório atual, **pare o backend** e copie a pasta `data` (verifique também `backend/data` se o IntelliJ usou outro diretório de execução). A versão anterior usa tipicamente `financeiro.mv.db`.

No terminal da raiz do repositório:

```powershell
git status
git switch -c feat/espirix-v03
```

**Não apague o banco anterior e não rode uma migração manual sobre ele.** O novo H2 usa `./data/espirix-v03.mv.db` e inicia vazio. O banco anterior permanece intacto.

## 2. Aplicar o PATCH — recomendado para o projeto que você já tem

O arquivo `Espirix-v0.3-PATCH.zip` contém caminhos como `backend/src/...` e `frontend/src/...`, relativos à raiz do repositório. Extraia **na raiz** do projeto existente. No Windows, é mais fácil abrir o ZIP e copiar as pastas `backend` e `frontend` para a raiz, escolhendo *substituir* arquivos correspondentes.

Evite substituir o `package.json` atual do frontend sem necessidade: o PATCH atualiza os arquivos de código, mas o seu `package.json` já tem as dependências React/Vite/Tailwind instaladas. Para Java, o PATCH altera `backend/pom.xml` e adiciona a dependência `spring-boot-starter-security`.

Depois, confira o resultado:

```powershell
git diff --stat
git status
```

O ZIP `Espirix-v0.3-COMPLETO.zip` é uma **cópia independente** para estudar ou executar do zero. Não substitua o projeto atual inteiro por ele sem comparar modificações que você fez depois da versão de origem.

## 3. Configurar senha do AdminMaster

No IntelliJ: `Run > Edit Configurations` → selecione sua configuração Spring Boot → `Environment variables`. Configure:

```
ESPIRIX_MASTER_EMAIL=seu-email@exemplo.com
ESPIRIX_MASTER_PASSWORD=uma-senha-nova-e-forte-com-16-ou-mais-caracteres
```

Não use o e-mail ou senha reais no README, prints, código, GitHub ou mensagens. A conta AdminMaster só é criada automaticamente quando ainda não existe no banco; atualizar a variável depois não troca a senha já cadastrada.

**Alternativa pelo PowerShell:** use `scripts/iniciar-backend.ps1 -Email "seu-email@exemplo.com"`, que solicita a senha sem exibi-la e inicia o Maven. Execute na raiz do projeto.

## 4. Rodar backend e frontend localmente

Na raiz:

```powershell
mvn.cmd -f backend/pom.xml clean test
mvn.cmd -f backend/pom.xml spring-boot:run
```

Se executar o backend pelo IntelliJ, aponte o *Working directory* para a **raiz do projeto**, para o arquivo H2 ser salvo em `data/espirix-v03.mv.db`.

Em outro terminal:

```powershell
cd frontend
npm.cmd install
npm.cmd run build
npm.cmd run dev
```

Abra `http://localhost:5173`. Faça login com a conta AdminMaster. Não se preocupe se o painel estiver vazio: o novo banco é independente do banco de teste anterior.

> **Se o Maven reclamar do `pom.xml`, do package Java ou de um teste, não prossiga para dados reais.** O backend atual no seu PC pode ter mudanças posteriores ao pacote original. Resolva a diferença antes de publicar.

## 5. Roteiro de teste de isolamento

1. Entre como AdminMaster: workspace inicial **Demonstração**.
2. Importe `examples/omie-exemplo-ficticio.csv`. O Dashboard Demo deve mostrar valores e títulos.
3. No cabeçalho, alterne para **Ambiente de trabalho**. O Dashboard e o histórico de importações devem estar vazios.
4. Em `Preferências > Gerenciar equipe`, crie a conta do seu colega, papel **Financeiro**, associada ao ambiente Trabalho. Use senha inicial única e repasse-a com segurança.
5. Saia e entre com a conta Financeiro: ela só deve conseguir ver o workspace Trabalho e **não** deve exibir seletor de empresas nem zona de perigo.
6. Volte ao AdminMaster, selecione Demo e clique `Preferências > Zona de perigo`. Confira os números e digite `APAGAR DEMO`. A página deverá mostrar zero registros para Demo. Confirme que Trabalho está intacto.
7. Com AdminMaster em Trabalho, a limpeza deve aparecer **bloqueada**. Só para dados de teste autorizados, configure `ESPIRIX_PERMITIR_LIMPEZA_TRABALHO=true` e reinicie o backend; a frase exigida será `APAGAR TRABALHO`.
8. Com os dados fictícios, tente acessar pela URL um título que só existe no outro workspace: o backend deve devolver **404**, não o dado alheio. Tente baixa com o ID alheio: também deve falhar.
9. Verifique que `/api/dashboard/resumo` sem login devolve **401** e que POST sem token CSRF não é aceito.

Se qualquer teste falhar, suspenda o teste com dados reais.

## 6. Como publicar com Docker (laboratório)

Os computadores **clientes** não instalam Java: apenas abrem a URL. É a máquina *servidora* que executa a API e o banco.

Para experimentar Docker no próprio PC:

```powershell
Copy-Item .env.example .env
# Edite .env, configure DB_PASSWORD, ESPIRIX_MASTER_EMAIL, ESPIRIX_MASTER_PASSWORD
# Mantenha COOKIE_SECURE=false somente no teste HTTP local.
docker compose up --build -d
```

A URL local de laboratório será `http://localhost:8085`, exposta **somente em 127.0.0.1**. Ela não é acessível pelo seu colega em outra máquina, por segurança.

Para publicação com domínio próprio e HTTPS, a infraestrutura precisa de servidor externo, DNS apontando para ele, portas 80/443, HTTPS e políticas de backup e segurança. O arquivo `docker-compose.public.yml` oferece Caddy como referência de proxy TLS. A configuração HTTPS exige `COOKIE_SECURE=true` e o domínio real em `ESPIRIX_DOMAIN`.

**Não basta executar `docker compose` e liberar o serviço na internet para considerá-lo pronto para receber dados reais.**

## 7. Próximas verificações antes de dados da empresa

- Autorização formal da empresa para processar seus dados e análise de titularidade/licença do software desenvolvido durante relação de emprego.
- Migrar `ddl-auto=update` para **Flyway** ou migrações versionadas e fazer backup/restauração testados.
- Realizar testes e revisão de segurança (sessões, permissões, CSRF, recuperação de senhas, MFA do master, limitação de login, logs, auditoria das alterações, isolamento por empresa).
- Política de controle e retenção de arquivos e dados pessoais, conforme LGPD quando aplicável.
- Validar o parser CSV/PDF com amostras **anonimizadas**; PDF pode falhar ou interpretar incorretamente layouts desconhecidos.
- Configurar infraestrutura profissional, TLS, gestão de segredos, monitoramento, alertas e recuperação de desastres.
- Verificar explicitamente a titularidade e os termos de uso do software antes de tratá-lo como venda comercial à empregadora.

Nunca envie extratos, relatórios, banco `.mv.db`, backups nem `.env` para um repositório público. Para o LinkedIn, mantenha somente dados totalmente fictícios no workspace Demo.

## Endpoints novos

| Método | Caminho | Acesso |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Anônimo (prepara token de login) |
| POST | `/api/auth/login` | Form `username`, `password` e CSRF |
| POST | `/api/auth/logout` | Sessão + CSRF |
| GET | `/api/auth/me` | Autenticado |
| GET | `/api/auth/empresas` | AdminMaster |
| POST | `/api/auth/workspace` | AdminMaster, `{ "empresaId": 1 }` |
| GET | `/api/auth/usuarios` | Administradores, apenas empresa ativa |
| POST | `/api/auth/usuarios` | Administradores, cria usuário na empresa ativa |
| GET | `/api/admin/limpeza/preview` | Administradores da empresa ativa |
| POST | `/api/admin/limpeza/executar` | Administradores + confirmação exata; bloqueio adicional para Trabalho |

Os endpoints financeiros existentes permanecem com os mesmos caminhos `/api/...`, mas agora exigem login e são filtrados no servidor por empresa.

## Limitações declaradas desta versão

- A estrutura permite cadastrar outras empresas no futuro, mas **não foi criada ainda uma interface para provisionar novas empresas**. Demo e Trabalho são inicializadas automaticamente.
- O registro de auditoria implementado cobre a **limpeza**, não todos os pagamentos/importações/login/logout; isto ainda precisa evoluir antes de uso real.
- Sessões são locais ao processo Spring Boot; esta configuração opera com **uma instância** do backend, sem balanceamento multiinstância.
- Não há interface de alteração ou recuperação de senha, segundo fator, alertas operacionais, logs centralizados ou rotina automatizada de backup/restauração nesta entrega.
- A separação foi implementada por chave `empresa_id` e consultas filtradas; defesa adicional no PostgreSQL com **Row Level Security** é recomendada antes de oferecer o produto a vários clientes.
- Este pacote foi construído a partir do backend-base disponível e do frontend previamente entregue. Ele não substitui uma auditoria do repositório em seu PC nem uma bateria de testes Maven/React/HTTP, que devem ser executados na sua máquina.
