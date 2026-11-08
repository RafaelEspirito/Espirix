package br.com.espiritolabs.espirix.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "titulos", uniqueConstraints = @UniqueConstraint(name = "uk_titulo_natural",
    columnNames = {"cliente_id", "numero_nota", "numero_parcela", "tipo_operacao"}),
    indexes = {@Index(name = "ix_titulo_vencimento", columnList = "data_vencimento")})
@Getter @Setter @NoArgsConstructor
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "numero_nota", nullable = false, length = 100)
    private String numeroNota;
    @Column(name = "numero_parcela", nullable = false, length = 30)
    private String numeroParcela = "UNICA";
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Client cliente;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 15)
    private TipoOperacao tipoOperacao;
    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;
    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;
    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;
    @Column(name = "valor_original", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorOriginal;
    @Column(name = "valor_pago", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorPago = BigDecimal.ZERO.setScale(2);
    @Column(name = "saldo_devedor", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoDevedor;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTitulo status;
    @Enumerated(EnumType.STRING)
    @Column(name = "origem_arquivo", nullable = false, length = 5)
    private OrigemArquivo origemArquivo;
    @Version
    private Long versao;

    public StatusTitulo statusEm(LocalDate hoje) {
        if (saldoDevedor.signum() == 0) return StatusTitulo.PAGA;
        if (valorPago.signum() > 0) return StatusTitulo.PAGA_PARCIAL;
        if (dataVencimento.isBefore(hoje)) return StatusTitulo.VENCIDA;
        return StatusTitulo.A_VENCER;
    }

    public boolean emAtraso(LocalDate hoje) {
        return saldoDevedor.signum() > 0 && dataVencimento.isBefore(hoje);
    }

    public void recalcular(LocalDate hoje) {
        if (valorOriginal == null || valorPago == null || valorOriginal.signum() <= 0
                || valorPago.signum() < 0 || valorPago.compareTo(valorOriginal) > 0) {
            throw new IllegalArgumentException("Valores inconsistentes no título");
        }
        saldoDevedor = valorOriginal.subtract(valorPago).setScale(2);
        status = statusEm(hoje);
    }
}
