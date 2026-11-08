package br.com.espiritolabs.espirix.repository;
import br.com.espiritolabs.espirix.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserAccountRepository extends JpaRepository<UserAccount,Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    List<UserAccount> findByEmpresaIdOrderByNomeAsc(Long empresaId);
}
