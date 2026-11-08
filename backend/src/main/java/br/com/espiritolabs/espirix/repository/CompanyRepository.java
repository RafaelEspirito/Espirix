package br.com.espiritolabs.espirix.repository;
import br.com.espiritolabs.espirix.domain.Company;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface CompanyRepository extends JpaRepository<Company,Long> {
    Optional<Company> findBySlug(String slug);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Company e where e.id=:id")
    Optional<Company> findLockedById(@Param("id") Long id);
}
