package br.com.espiritolabs.espirix.domain;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter; import lombok.NoArgsConstructor;
@Entity @Table(name="usuarios", indexes=@Index(name="ix_usuario_empresa",columnList="empresa_id")) @Getter @Setter @NoArgsConstructor
public class UserAccount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=200) private String email;
    @Column(nullable=false,length=150) private String nome;
    @Column(name="senha_hash",nullable=false,length=100) private String senhaHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private UserRole perfil;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="empresa_id") private Company empresa;
    @Column(nullable=false) private boolean ativo=true;
}
