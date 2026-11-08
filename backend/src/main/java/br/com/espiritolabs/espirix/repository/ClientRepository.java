package br.com.espiritolabs.espirix.repository;

import br.com.espiritolabs.espirix.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {
    Optional<Client> findByChaveBusca(String chaveBusca);
}
