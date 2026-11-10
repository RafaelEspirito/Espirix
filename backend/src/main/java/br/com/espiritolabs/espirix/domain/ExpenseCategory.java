package br.com.espiritolabs.espirix.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categorias_despesa", uniqueConstraints = @UniqueConstraint(name = "uk_categoria_empresa_chave", columnNames = {"empresa_id", "chave"}))
@Getter @Setter @NoArgsConstructor
public class ExpenseCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "empresa_id", nullable = false, updatable = false) private Long empresaId;
    @Column(nullable = false, length = 100) private String nome;
    @Column(nullable = false, length = 100) private String chave;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 15) private Type tipo;
    @Column(nullable = false) private boolean ativa = true;
    public enum Type { FIXA, VARIAVEL }
}
