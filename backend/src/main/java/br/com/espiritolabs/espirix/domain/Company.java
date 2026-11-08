package br.com.espiritolabs.espirix.domain;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter; import lombok.NoArgsConstructor;
@Entity @Table(name="empresas") @Getter @Setter @NoArgsConstructor
public class Company {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=50) private String slug;
    @Column(nullable=false, length=150) private String nome;
}
