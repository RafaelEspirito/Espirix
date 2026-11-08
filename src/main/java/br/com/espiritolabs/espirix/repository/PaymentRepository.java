package br.com.espiritolabs.espirix.repository;

import br.com.espiritolabs.espirix.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByTituloIdOrderByDataPagamentoAscIdAsc(Long tituloId);
    List<Payment> findByTituloTipoOperacao(br.com.espiritolabs.espirix.domain.TipoOperacao tipoOperacao);
    List<Payment> findByTituloClienteIdAndTituloTipoOperacao(Long clienteId, br.com.espiritolabs.espirix.domain.TipoOperacao tipoOperacao);
}
