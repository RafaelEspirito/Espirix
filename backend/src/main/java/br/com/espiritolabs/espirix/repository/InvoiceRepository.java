package br.com.espiritolabs.espirix.repository;
import br.com.espiritolabs.espirix.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface InvoiceRepository extends JpaRepository<Invoice,Long>, JpaSpecificationExecutor<Invoice> {
    Optional<Invoice> findByEmpresaIdAndClienteIdAndNumeroNotaAndNumeroParcelaAndTipoOperacao(
        Long empresaId, Long clienteId, String numeroNota, String parcela, TipoOperacao tipoOperacao);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id=:id and i.empresaId=:empresaId")
    Optional<Invoice> findLockedByIdAndEmpresaId(@Param("id") Long id,@Param("empresaId") Long empresaId);
    Optional<Invoice> findByIdAndEmpresaId(Long id, Long empresaId);
    List<Invoice> findByEmpresaIdAndTipoOperacao(Long empresaId,TipoOperacao tipoOperacao);
    List<Invoice> findByEmpresaIdAndClienteIdAndTipoOperacao(Long empresaId,Long clienteId,TipoOperacao tipoOperacao);
    long countByEmpresaId(Long empresaId);
    @Modifying @Query("delete from Invoice i where i.empresaId=:empresaId")
    int apagarEmpresa(@Param("empresaId") Long empresaId);
}
