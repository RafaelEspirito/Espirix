# Contrato REST — Etapa 1

Base URL: `http://localhost:8080`. Respostas JSON. Datas `YYYY-MM-DD`, valores monetários como números decimais sem conversão para `double` no Java.

| Método | Endpoint | Parâmetros / corpo | Aplicação |
|---|---|---|---|
| GET | `/actuator/health` | — | Saúde da aplicação |
| GET | `/api/dashboard/resumo` | `tipoOperacao=RECEBER\|PAGAR` | KPIs globais |
| GET | `/api/dashboard/evolucao` | `tipoOperacao`, `meses=12` (1–60) | Série mensal do saldo |
| GET | `/api/dashboard/previsao` | `tipoOperacao` | Faixas de vencimentos futuros |
| GET | `/api/dashboard/top-devedores` | `tipoOperacao`, `limite=5` | Maiores saldos por contraparte |
| GET | `/api/clientes` | `busca`, `page`, `size` | Catálogo de contrapartes |
| GET | `/api/clientes/consolidado` | `busca`, `emissaoDe`, `emissaoAte`, `vencimentoDe`, `vencimentoAte`, `status`, `tipoOperacao` | Dívida consolidada e % quitado |
| GET | `/api/clientes/{id}` | — | Detalhes do cliente/fornecedor |
| GET | `/api/clientes/{id}/evolucao` | `meses`, `tipoOperacao` | Série do cliente |
| GET | `/api/titulos` | `busca`, `emissaoDe`, `emissaoAte`, `vencimentoDe`, `vencimentoAte`, `status`, `tipoOperacao`, `somenteAbertos`, `page`, `size` | Tabela operacional paginada |
| GET | `/api/titulos/{id}` | — | Detalhes da nota |
| GET | `/api/titulos/{id}/pagamentos` | — | Histórico de baixas da nota |
| POST | `/api/titulos/{id}/pagamentos` | `{valor, dataPagamento, observacao}` | Baixa parcial/total |
| POST | `/api/titulos/pagamentos/lote` | `{pagamentos: [{tituloId,valor,dataPagamento}], observacao}` | Baixas com valores distintos, atômicas |
| POST | `/api/titulos/quitar-lote` | `{titulosIds: [1,2],dataPagamento,observacao}` | Quita o saldo remanescente de várias notas |
| POST | `/api/importacoes` | multipart `arquivo`, parâmetro opcional `tipoOperacao` | Importação CSV/PDF |
| GET | `/api/importacoes` | `page`, `size` | Histórico de arquivos e erros |

## Resposta ilustrativa do KPI

```json
{
  "tipoOperacao": "RECEBER",
  "dataReferencia": "2026-11-08",
  "dividaAtiva": 3200.00,
  "totalVencido": 800.00,
  "aVencer7Dias": 200.00,
  "aVencer15Dias": 1250.00,
  "aVencer30Dias": 2400.00,
  "totalQuitadoMes": 0.00,
  "totalPagamentosMes": 100.00,
  "quantidadeTitulosAbertos": 4
}
```

*Valores acima são inteiramente fictícios, somente para ilustrar o contrato.*

## Filtros

- Os filtros de data usam ISO 8601, ex.: `?emissaoDe=2026-01-01&emissaoAte=2026-12-31`.
- `busca` procura razão social, nome fantasia e, em `/api/titulos`, número de documento.
- `status`: `A_VENCER`, `VENCIDA`, `PAGA` e `PAGA_PARCIAL`.
- `tipoOperacao` tem valor padrão `RECEBER` nos endpoints operacionais; escolha `PAGAR` para contas a pagar.
- Paginação a partir de zero, até 100 registros por página.
- `top-devedores?tipoOperacao=PAGAR` retorna maiores obrigações por fornecedor, apesar do nome legado do endpoint.

## Erros

HTTP `400` para entrada inválida; `404` para ID inexistente; `413` para upload acima do limite. Exemplo:

```json
{
  "timestamp": "2026-11-08T09:00:00",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Pagamento supera o saldo ou e nao positivo: titulo 12",
  "caminho": "/api/titulos/12/pagamentos",
  "detalhes": []
}
```

Há também documentação navegável no Swagger: `/swagger-ui.html`.
