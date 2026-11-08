package br.com.espiritolabs.espirix.domain;
import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter; import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Entity @Table(name="auditoria", indexes=@Index(name="ix_auditoria_empresa",columnList="empresa_id")) @Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="empresa_id",nullable=false) private Long empresaId;
    @Column(name="usuario_id",nullable=false) private Long usuarioId;
    @Column(nullable=false,length=60) private String acao;
    @Column(nullable=false,length=600) private String detalhes;
    @Column(nullable=false) private LocalDateTime registradoEm=LocalDateTime.now();
}
