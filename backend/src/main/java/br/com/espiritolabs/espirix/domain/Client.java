package br.com.espiritolabs.espirix.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes", uniqueConstraints = @UniqueConstraint(name = "uk_cliente_chave", columnNames = "chave_busca"))
@Getter @Setter @NoArgsConstructor
public class Client {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;
    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia;
    @Column(name = "cnpj_cpf", length = 14)
    private String cnpjCpf;
    @Column(name = "chave_busca", nullable = false, length = 220)
    private String chaveBusca;
    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;
    @PrePersist
    void prePersist() { if (dataCadastro == null) dataCadastro = LocalDateTime.now(); }
}
