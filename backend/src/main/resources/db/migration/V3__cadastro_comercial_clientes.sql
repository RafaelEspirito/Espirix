-- v0.6: informacoes opcionais. Nenhum cliente existente e apagado.
ALTER TABLE clientes ADD COLUMN email_cobranca VARCHAR(200);
ALTER TABLE clientes ADD COLUMN responsavel_venda VARCHAR(150);
