package br.com.portfolio.financeiro.importer;

import br.com.portfolio.financeiro.domain.TipoOperacao;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Um registro de entrada normalizado, independente do formato de arquivo. */
public record ImportedInvoice(String razaoSocial, String nomeFantasia, String cnpjCpf,
                              String numeroNota, String numeroParcela, TipoOperacao tipoOperacao,
                              LocalDate dataEmissao, LocalDate dataVencimento, LocalDate dataPagamento,
                              BigDecimal valorOriginal, BigDecimal valorPago) { }
