package br.com.portfolio.financeiro.repository;

import br.com.portfolio.financeiro.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByTituloIdOrderByDataPagamentoAscIdAsc(Long tituloId);
    List<Payment> findByTituloTipoOperacao(br.com.portfolio.financeiro.domain.TipoOperacao tipoOperacao);
    List<Payment> findByTituloClienteIdAndTituloTipoOperacao(Long clienteId, br.com.portfolio.financeiro.domain.TipoOperacao tipoOperacao);
}
