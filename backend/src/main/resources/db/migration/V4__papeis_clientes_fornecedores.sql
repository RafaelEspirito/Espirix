-- Um unico cadastro pode representar cliente e fornecedor.
-- Preserva todos os IDs, titulos e pagamentos existentes.
ALTER TABLE clientes ADD COLUMN eh_cliente BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE clientes ADD COLUMN eh_fornecedor BOOLEAN NOT NULL DEFAULT FALSE;
-- Titulos de contas a pagar identificam fornecedores ja existentes.
UPDATE clientes c SET eh_fornecedor = TRUE
WHERE EXISTS (SELECT 1 FROM titulos t WHERE t.empresa_id = c.empresa_id
              AND t.cliente_id = c.id AND t.tipo_operacao = 'PAGAR');
-- Se a contraparte so tem titulos a pagar, ela e fornecedor (nao cliente).
UPDATE clientes c SET eh_cliente = FALSE
WHERE eh_fornecedor = TRUE AND NOT EXISTS (
    SELECT 1 FROM titulos t WHERE t.empresa_id = c.empresa_id
    AND t.cliente_id = c.id AND t.tipo_operacao = 'RECEBER');
CREATE INDEX idx_clientes_papeis_empresa ON clientes(empresa_id, eh_cliente, eh_fornecedor);
