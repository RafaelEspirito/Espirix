package br.com.espiritolabs.espirix.repository;
import br.com.espiritolabs.espirix.domain.Client;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface ClientRepository extends JpaRepository<Client,Long>, JpaSpecificationExecutor<Client> {
    Optional<Client> findByEmpresaIdAndChaveBusca(Long empresaId, String chaveBusca);
    Optional<Client> findByIdAndEmpresaId(Long id, Long empresaId);
    boolean existsByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaId(Long empresaId);
    @Modifying @Query("delete from Client c where c.empresaId=:empresaId")
    int apagarEmpresa(@Param("empresaId") Long empresaId);
}
