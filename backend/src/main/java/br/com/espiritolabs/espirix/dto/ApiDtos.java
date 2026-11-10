package br.com.espiritolabs.espirix.dto;

import br.com.espiritolabs.espirix.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Contratos publicos da API; entidades JPA nunca sao serializadas diretamente. */
public final class ApiDtos {
    private ApiDtos() { }

    public record ClienteResponse(Long id, String razaoSocial, String nomeFantasia,
                                  String cnpjCpf, LocalDateTime dataCadastro,
                                  String emailCobranca, String responsavelVenda, boolean cliente, boolean fornecedor) { }
    public record TituloResponse(Long id, String numeroNota, String numeroParcela, ClienteResponse cliente,
                                 TipoOperacao tipoOperacao, LocalDate dataEmissao, LocalDate dataVencimento,
                                 LocalDate dataPagamento, BigDecimal valorOriginal, BigDecimal valorPago,
                                 BigDecimal saldoDevedor, StatusTitulo status, boolean emAtraso,
                                 OrigemArquivo origemArquivo, Long categoriaId, String categoriaNome,
                                 String descricao, String observacao) { }
    public record PagamentoResponse(Long id, Long tituloId, BigDecimal valor, LocalDate dataPagamento,
                                    OrigemPagamento origem, String observacao, LocalDateTime registradoEm,
                                    LocalDateTime estornadoEm, String motivoEstorno) { }
    public record ImportacaoResponse(Long id, String nomeArquivo, OrigemArquivo tipoArquivo,
                                     LocalDateTime dataImportacao, int quantidadeRegistrosNovos,
                                     int quantidadeAtualizados, int quantidadeIgnorados,
                                     StatusImportacao status, String mensagem) { }
    public record PagamentoRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal valor,
                                   @NotNull @PastOrPresent LocalDate dataPagamento,
                                   @Size(max = 300) String observacao) { }
    public record EstornoRequest(@NotBlank @Size(max = 300) String motivo) { }
    public record ItemBaixaRequest(@NotNull Long tituloId, @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal valor,
                                   @NotNull @PastOrPresent LocalDate dataPagamento) { }
    public record LotePagamentosRequest(@NotEmpty List<@Valid ItemBaixaRequest> pagamentos,
                                        @Size(max = 300) String observacao) { }
    public record QuitarLoteRequest(@NotEmpty List<@NotNull Long> titulosIds,
                                    @NotNull @PastOrPresent LocalDate dataPagamento,
                                    @Size(max = 300) String observacao) { }
    public record ConsolidadoCliente(Long clienteId, String razaoSocial, String nomeFantasia,
                                     long quantidadeNotasEmAberto, BigDecimal valorOriginal,
                                     BigDecimal valorPago, BigDecimal saldoDevedor,
                                     BigDecimal percentualQuitado) { }
    public record ResumoDashboard(TipoOperacao tipoOperacao, LocalDate dataReferencia,
                                  BigDecimal dividaAtiva, BigDecimal totalVencido,
                                  BigDecimal aVencer7Dias, BigDecimal aVencer15Dias,
                                  BigDecimal aVencer30Dias, BigDecimal totalQuitadoMes,
                                  BigDecimal totalPagamentosMes, long quantidadeTitulosAbertos) { }
    public record PontoEvolucao(String periodo, BigDecimal novasNotas,
                                BigDecimal pagamentos, BigDecimal saldoAcumulado) { }
    public record FaixaPrevisao(String faixa, BigDecimal valor, long quantidade) { }
    public record Devedor(Long clienteId, String nomeFantasia, String razaoSocial,
                          BigDecimal saldoDevedor, long quantidadeNotasEmAberto) { }
}
