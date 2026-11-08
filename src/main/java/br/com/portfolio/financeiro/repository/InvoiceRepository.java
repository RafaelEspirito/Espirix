package br.com.portfolio.financeiro.repository;

import br.com.portfolio.financeiro.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
    Optional<Invoice> findByClienteIdAndNumeroNotaAndNumeroParcelaAndTipoOperacao(
            Long clienteId, String numeroNota, String parcela, TipoOperacao tipoOperacao);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findLockedById(@Param("id") Long id);

    List<Invoice> findByTipoOperacao(TipoOperacao tipoOperacao);
    List<Invoice> findByClienteIdAndTipoOperacao(Long clienteId, TipoOperacao tipoOperacao);
}
