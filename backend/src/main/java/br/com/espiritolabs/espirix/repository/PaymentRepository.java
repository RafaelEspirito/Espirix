package br.com.espiritolabs.espirix.repository;
import br.com.espiritolabs.espirix.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface PaymentRepository extends JpaRepository<Payment,Long> {
    List<Payment> findByEmpresaIdAndTituloIdOrderByDataPagamentoAscIdAsc(Long empresaId, Long tituloId);
    List<Payment> findByEmpresaIdAndTituloTipoOperacao(Long empresaId, TipoOperacao tipoOperacao);
    List<Payment> findByEmpresaIdAndTituloClienteIdAndTituloTipoOperacao(Long empresaId,Long clienteId,TipoOperacao tipoOperacao);
    long countByEmpresaId(Long empresaId);
    @Modifying @Query("delete from Payment p where p.empresaId=:empresaId")
    int apagarEmpresa(@Param("empresaId") Long empresaId);
}
