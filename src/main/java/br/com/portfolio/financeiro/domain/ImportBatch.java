package br.com.portfolio.financeiro.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "importacoes", indexes = @Index(name = "ix_importacao_data", columnList = "data_importacao"))
@Getter @Setter @NoArgsConstructor
public class ImportBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "nome_arquivo", nullable = false, length = 255)
    private String nomeArquivo;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_arquivo", nullable = false, length = 5)
    private OrigemArquivo tipoArquivo;
    @Column(name = "data_importacao", nullable = false, updatable = false)
    private LocalDateTime dataImportacao;
    @Column(name = "quantidade_registros_novos", nullable = false)
    private int quantidadeRegistrosNovos;
    @Column(name = "quantidade_atualizados", nullable = false)
    private int quantidadeAtualizados;
    @Column(name = "quantidade_ignorados", nullable = false)
    private int quantidadeIgnorados;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusImportacao status;
    @Column(length = 600)
    private String mensagem;
    @Column(name = "sha256", length = 64)
    private String sha256;
    @PrePersist
    void prePersist() { if (dataImportacao == null) dataImportacao = LocalDateTime.now(); }
}
