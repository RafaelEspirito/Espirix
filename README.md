# OMIE Financeiro — Backend (Etapa 1/3)

API REST em **Java 21 + Spring Boot 3.5** para importar relatórios exportados do ERP OMIE, acompanhar títulos financeiros, registrar baixas parciais/totais e fornecer indicadores para um dashboard React.

> **Projeto educacional / portfólio, não pronto para produção.** Sem autenticação, autorização por usuário, auditoria regulatória completa, isolamento multitenant, controle de permissão de acesso aos dados e migrações versionadas. **Não importe dados reais/confidenciais em um servidor público**. O modo privacidade da Etapa 2 mascara *somente a interface*, não oculta conteúdo da API/BD.

## Requisitos

- JDK 21 (`java -version`)
- Maven 3.6.3 ou superior (`mvn -version`)
- Não precisa instalar H2 — dependência embarcada

## Executar

```bash
mvn clean test
mvn spring-boot:run
```

API: <http://localhost:8080/api/dashboard/resumo>  
Documentação interativa: <http://localhost:8080/swagger-ui.html>  
OpenAPI JSON: <http://localhost:8080/v3/api-docs>  
Health: <http://localhost:8080/actuator/health>

O banco H2 é criado em `./data/financeiro.mv.db` e permanece após reiniciar o aplicativo (diretório ignorado pelo Git).

## Importar relatório fictício

```bash
curl -X POST 'http://localhost:8080/api/importacoes' \
  -F 'arquivo=@examples/omie-exemplo-ficticio.csv'

# Para forçar a categoria de todos os registros, mesmo que o arquivo não possua coluna 'Tipo Operação':
curl -X POST 'http://localhost:8080/api/importacoes?tipoOperacao=PAGAR' \
  -F 'arquivo=@examples/omie-exemplo-ficticio.csv'
```

**Atenção:** o segundo exemplo é didático; em uso real, importe arquivos de recebíveis e pagamentos separados e selecione o tipo correto. O `tipoOperacao` de cada registro determina se os valores entram em contas a receber ou a pagar. Sem coluna nem parâmetro, o padrão do adaptador é `RECEBER`.

### CSV esperado

Cabeçalhos podem variar entre aliases configurados em `ReportRowMapper`. Campos necessários: `Razão Social` (ou `Cliente`), `Número Documento`, `Data Emissão`, `Data Vencimento` e `Valor Original`. Campos opcionais: `Nome Fantasia`, `CNPJ/CPF`, `Parcela`, `Tipo Operação`, `Valor Pago`, `Data Pagamento`, `Situação`.

- Delimitadores: `;`, `,` ou tab. Charset: UTF-8 (com/sem BOM) ou Windows-1252.
- Formatos de data: `dd/MM/yyyy`, `yyyy-MM-dd` ou data/hora com data no início.
- Valores: `1.234,56` e `1234.56`; o separador `.` isolado seguido de 3 dígitos é tratado como milhar no padrão brasileiro.
- `Valor Pago` é **total acumulado baixado no ERP**; o arquivo precisa de `Data Pagamento` se houver valor pago. Quando ausente, `Situação=PAGO/QUITADO` interpreta valor pago como valor total.
- Uma nota parcelada exige preencher `Parcela` em cada linha. Ex.: `NF-1001` com parcelas `01/02` e `02/02`. Sem parcela, usa `UNICA`.
- Duplicidade: chave composta `(cliente, número do documento, parcela, tipo operação)`; cliente é identificado pelo CNPJ/CPF quando disponível e, caso contrário, pelo nome normalizado. Essa escolha evita misturar parcelas distintas, mas a mudança de nome/CNPJ em relatórios incompletos exige conciliação manual.
- Importações são **atômicas**: erro em qualquer linha reverte todas as alterações daquele arquivo. A falha é registrada no histórico de importações.

### PDF: limitações reais

PDFBox **não é um extrator semântico universal de tabelas**. Este backend implementa um parser para **PDFs com texto selecionável e colunas separadas/alinhadas**. Primeiro tenta reconstruir colunas pelos espaços do texto; depois usa as coordenadas X/Y dos caracteres. Se a disposição do relatório não for compatível, **rejeita o arquivo** (em vez de criar lançamentos incorretos). Não possui OCR para imagens escaneadas, nem foi calibrado com um relatório PDF real do OMIE.

Para suportar precisamente seu relatório OMIE, salve/exporte um **exemplo anonimizado** desse PDF e ajuste o adaptador `PdfReportParser` / `PdfGeometryExtractor` conforme cabeçalho, posição das colunas, quebra de página, totais e possíveis linhas multilinhas. Um CSV normalmente é a opção mais segura quando disponível.

## Baixas e evolução

Pagamento parcial:

```bash
curl -X POST 'http://localhost:8080/api/titulos/1/pagamentos' \
  -H 'Content-Type: application/json' \
  -d '{"valor":100.00,"dataPagamento":"2026-11-08","observacao":"Pix confirmado"}'
```

Pagamento de várias notas com valores individuais:

```bash
curl -X POST 'http://localhost:8080/api/titulos/pagamentos/lote' \
  -H 'Content-Type: application/json' \
  -d '{"observacao":"Conciliação bancária", "pagamentos":[{"tituloId":1,"valor":50,"dataPagamento":"2026-11-08"},{"tituloId":2,"valor":200,"dataPagamento":"2026-11-08"}]}'
```

Quitar completamente vários títulos:

```bash
curl -X POST 'http://localhost:8080/api/titulos/quitar-lote' \
  -H 'Content-Type: application/json' \
  -d '{"titulosIds":[1,2],"dataPagamento":"2026-11-08","observacao":"Quitação em lote"}'
```

Datas devem ser válidas (não futuras e não anteriores à emissão). Valores devem ser positivos e limitados pelo saldo aberto. Cada baixa gera evento `Payment`, possibilitando o cálculo da série temporal de emissões menos pagamentos.

**Reimportação:** se o total pago indicado no ERP for maior que as baixas já registradas, cria um pagamento de reconciliação apenas pela **diferença**. Se o total do ERP for menor, mantém as baixas existentes — relatórios antigos não estornam baixas manuais. Ajustes e estornos formais devem ganhar um fluxo específico numa evolução posterior.

## Modelo de status

- `PAGA`: saldo igual a zero.
- `PAGA_PARCIAL`: recebeu algum pagamento, mas há saldo restante.
- `VENCIDA`: aberta, sem baixa parcial, com vencimento anterior à data atual.
- `A_VENCER`: aberta, sem baixa parcial, com vencimento hoje ou no futuro.

Um título `PAGA_PARCIAL` também pode estar **em atraso**: seu DTO traz `emAtraso` booleano, usado em KPIs e filtros por vencimento. Portanto, o filtro `status=VENCIDA` inclui títulos parcialmente pagos e vencidos, mesmo que o DTO continue exibindo `PAGA_PARCIAL`. Status é atualizado no banco diariamente e também calculado na leitura para evitar divergência na virada do dia.

## Métricas

- `dividaAtiva`: soma do saldo de todos os títulos abertos, separados por `tipoOperacao`.
- `totalVencido`: soma do saldo de todos os abertos com vencimento anterior à data atual (inclusive parcialmente pagos).
- `aVencer7/15/30Dias`: faixas **cumulativas**, considerando vencimentos entre hoje e D+N, inclusive.
- `totalQuitadoMes`: soma do **valor original** dos títulos integralmente quitados cuja última baixa ocorreu no mês atual.
- `totalPagamentosMes`: soma dos **pagamentos efetivos** (inclusive parciais) registrados com data no mês atual.
- `previsao`: faixas **exclusivas** de 0–7, 8–15, 16–30 e 31+ dias.
- `evolucao`: reconstrução cronológica das notas pela data de emissão e pagamentos pela data da baixa. O saldo acumulado inclui eventos anteriores ao período exibido; pode divergir de snapshots contábeis históricos se notas forem editadas retroativamente. Não representa saldo por *data de importação*.

## PostgreSQL

```bash
SPRING_PROFILES_ACTIVE=postgres \
DB_URL=jdbc:postgresql://localhost:5432/omie_financeiro \
DB_USER=postgres DB_PASSWORD=senha-forte \
mvn spring-boot:run
```

As credenciais devem estar em variáveis de ambiente, nunca no Git. O esquema é gerenciado por `ddl-auto=update` apenas por conveniência de desenvolvimento. Antes de produção, migrar para Flyway, `ddl-auto=validate`, autenticação/autorização, segregação de contas, trilha de auditoria imutável, backups e controle de idempotência de requests.

**Limitação de escala:** relatórios de dashboard e consolidação carregam lançamentos na memória para prototipação. Com grandes bases, substitua por consultas agregadas/paginadas no PostgreSQL e materialização de séries de eventos.

A lista completa dos endpoints está em [`docs/API.md`](docs/API.md), e as decisões do modelo de dados em [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md).
