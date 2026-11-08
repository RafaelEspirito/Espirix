package br.com.espiritolabs.espirix.service;

import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.dto.ApiDtos.*;
import br.com.espiritolabs.espirix.exception.BusinessException;
import br.com.espiritolabs.espirix.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoices;
    private final ClientRepository clients;
    private final PaymentRepository payments;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<TituloResponse> listar(InvoiceFilters.Filtro filtro, Pageable page) {
        return invoices.findAll(InvoiceFilters.build(filtro, LocalDate.now(clock)), page)
                .map(i -> DtoMapper.titulo(i, LocalDate.now(clock)));
    }
    @Transactional(readOnly = true)
    public TituloResponse buscar(Long id) {
        return DtoMapper.titulo(find(id), LocalDate.now(clock));
    }
    @Transactional(readOnly = true)
    public Page<ClienteResponse> clientes(String busca, Pageable page) {
        Specification<Client> spec = (root, query, cb) -> {
            if (busca == null || busca.isBlank()) return cb.conjunction();
            String term = "%" + busca.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(root.get("razaoSocial")), term),
                cb.like(cb.lower(root.get("nomeFantasia")), term));
        };
        return clients.findAll(spec, page).map(DtoMapper::cliente);
    }
    @Transactional(readOnly = true)
    public ClienteResponse cliente(Long id) { return DtoMapper.cliente(clients.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Cliente nao encontrado: " + id))); }
    @Transactional(readOnly = true)
    public List<PagamentoResponse> historico(Long tituloId) {
        find(tituloId);
        return payments.findByTituloIdOrderByDataPagamentoAscIdAsc(tituloId)
                .stream().map(DtoMapper::pagamento).toList();
    }
    private Invoice find(Long id) { return invoices.findById(id)
            .orElseThrow(() -> BusinessException.notFound("Titulo nao encontrado: " + id)); }

    @Scheduled(cron = "0 5 0 * * *", zone = "${app.timezone:America/Bahia}")
    @Transactional
    public void atualizarStatusViradaDia() {
        LocalDate hoje = LocalDate.now(clock);
        for (Invoice invoice : invoices.findAll()) {
            StatusTitulo atual = invoice.statusEm(hoje);
            if (invoice.getStatus() != atual) invoice.setStatus(atual);
        }
    }
}
