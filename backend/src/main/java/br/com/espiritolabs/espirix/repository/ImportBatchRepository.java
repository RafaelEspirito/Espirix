package br.com.espiritolabs.espirix.repository;
import br.com.espiritolabs.espirix.domain.ImportBatch;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ImportBatchRepository extends JpaRepository<ImportBatch,Long> {
    Page<ImportBatch> findByEmpresaId(Long empresaId, Pageable pageable);
    long countByEmpresaId(Long empresaId);
    @Modifying @Query("delete from ImportBatch b where b.empresaId=:empresaId")
    int apagarEmpresa(@Param("empresaId") Long empresaId);
}
