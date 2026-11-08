package br.com.espiritolabs.espirix.repository;
import br.com.espiritolabs.espirix.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog,Long> { }
