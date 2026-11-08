# Espirix Cloud — Checklist obrigatório antes de dados reais

Registre data, responsável, evidência e aceite por item. Sem todos os críticos aprovados, use apenas dados fictícios.

- [ ] Autorização escrita da empresa para uso de dados, hospedagem, responsabilidades e propriedade do software.
- [ ] Domínio/DNS corretos; HTTPS válido; redirecionamento HTTP → HTTPS; cookie Secure, HttpOnly e SameSite.
- [ ] Nenhuma porta 5432/8080 do PostgreSQL/backend exposta; firewall revisada.
- [ ] MFA habilitado para acesso administrativo (a versão NÃO traz MFA nativo).
- [ ] Credenciais únicas; antiga senha exposta em captura foi substituída; `.env` protegido e não versionado.
- [ ] Revisão de dependências (SCA), OWASP ASVS e teste de autorização horizontal (IDOR) em todas as APIs.
- [ ] Teste de login incorreto e rate-limit, CSRF e logout entre navegadores.
- [ ] Rafael e Micael com logins distintos e dados compartilhados da mesma empresa.
- [ ] Financeiro impedido de listar/trocar empresas e acessar registros de outra empresa por URL/API.
- [ ] Importação incremental, pagamento parcial, reimportação e conciliação com arquivos fictícios.
- [ ] Exclusão total no Trabalho negada até para ADMIN_MASTER em cloud.
- [ ] Banco PostgreSQL migrado com Flyway, `ddl-auto=validate`, app user sem superuser.
- [ ] Backup AUTOMATIZADO agendado, alerta em falha e retenção definida.
- [ ] Backup externo criptografado; restaurado em ambiente isolado com sucesso.
- [ ] Teste de reinício de containers sem perda de registros e teste de recuperação.
- [ ] Logs e auditoria revistos, retenção de dados e controle de acesso ao servidor.
- [ ] Segurança e responsabilidade contratual/LGPD avaliadas por profissional competente.
- [ ] Aceite formal de usuários e da empresa antes de importar qualquer dado real.

**Atenção:** este checklist não é certificado de conformidade; é a base para homologação.
