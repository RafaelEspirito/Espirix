package br.com.espiritolabs.espirix.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos", indexes = @Index(name = "ix_pagamento_data", columnList = "data_pagamento"))
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private Long empresaId;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "titulo_id", nullable = false)
    private Invoice titulo;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;
    @Column(name = "usuario_id")
    private Long usuarioId;
    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;
    @Column(name = "registrado_em", nullable = false, updatable = false)
    private LocalDateTime registradoEm;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private OrigemPagamento origem;
    @Column(length = 300)
    private String observacao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "importacao_id")
    private ImportBatch importacao;
    @PrePersist
    void prePersist() { if (registradoEm == null) registradoEm = LocalDateTime.now(); }
}
