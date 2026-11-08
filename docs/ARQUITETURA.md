# Arquitetura e modelo de dados

## Camadas

```text
React (etapas 2 e 3) ──HTTP/JSON──> Controller ──> Service ──> Repository ──> JPA/H2 ou PostgreSQL
                                           │
                                        Importer
                                     CSV / PDFBox
```

- `controller`: valida entrada HTTP e devolve DTOs, sem expor entidades.
- `dto`: contratos JSON imutáveis em `record`s.
- `service`: regras de pagamento, importação transacional, filtros e analytics.
- `importer`: adapters de formatos para `ImportedInvoice`, independentes de JPA.
- `repository`: consultas com Spring Data JPA / Specification.
- `domain`: entidades, relações e enumeradores.
- `config`: CORS configurável e relógio testável.
- `exception`: erro HTTP padronizado.

## Relacionamentos

```text
Client (1) ── (N) Invoice (1) ── (N) Payment
                       │                  │
                       │                  └── (0..1) ImportBatch
                       │
                       └── tipoOperacao = RECEBER/PAGAR

ImportBatch armazena sucesso/falha e contagem de novos/atualizados/ignorados.
```

A `Invoice` guarda o valor original, o total pago e o saldo atual para consultas rápidas. Cada `Payment` é um evento datado e não deve ser apagado para "corrigir" valores. A evolução temporal é reconstruída das notas emitidas e eventos de pagamento. A linha `valorPago` importada é um snapshot cumulativo: somente o delta acima dos pagamentos já conhecidos é acrescentado ao histórico.

A chave única do título inclui `cliente_id, numero_nota, numero_parcela, tipo_operacao`. O nome `Client` é mantido em respeito ao contrato solicitado: nos lançamentos `PAGAR`, essa entidade representa o fornecedor/contraparte, e **o saldo de pagar nunca é somado à dívida de clientes**.

## Decisões e restrições

1. **Integridade monetária:** `BigDecimal` com escala 2 e comparações exatas, sem `float` ou `double`.
2. **Transação:** importações e lotes de pagamento são atômicos; qualquer falha reverte as alterações.
3. **Conciliação:** importar arquivo antigo nunca desfaz baixa manual.
4. **Concorrência:** baixas obtêm locks pessimistas de títulos em ordem consistente. Os títulos também possuem versão JPA otimista.
5. **Status:** `PAGA_PARCIAL` é status de liquidação; `emAtraso` indica atraso cronológico independente.
6. **Segurança:** somente desenvolvimento local nesta etapa. Modo privacidade do React não substitui autenticação nem anonimização dos dados na API.
7. **Formato variável:** PDFBox extrai texto/geometria; sem documento real do OMIE, o parser não presume que todos os layouts sejam compatíveis.
8. **Dados históricos:** renumerar nota, corrigir parcela ou trocar o identificador do cliente pode criar novo título. Uma rotina de conciliação de identidades é melhoria futura.

## Evoluções arquiteturais recomendadas

Antes de ambiente corporativo real: JWT/OIDC + RBAC, auditoria imutável (inclusive reversões), controle de idempotência de requisições, Flyway, paginar e agregar em SQL, testes contra PDF real, observabilidade, backups e restauração, HTTPS, limitação de requisições, separação por organização/tenant e LGPD.
