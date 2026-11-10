package br.com.espiritolabs.espirix.repository;
import br.com.espiritolabs.espirix.domain.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    List<ExpenseCategory> findByEmpresaIdOrderByNomeAsc(Long empresaId);
    Optional<ExpenseCategory> findByIdAndEmpresaId(Long id, Long empresaId);
    boolean existsByEmpresaIdAndChave(Long empresaId, String chave);
}
