package br.com.espiritolabs.espirix.service;

import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.dto.ApiDtos.*;
import br.com.espiritolabs.espirix.exception.BusinessException;
import br.com.espiritolabs.espirix.repository.*;
import br.com.espiritolabs.espirix.util.FinanceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class PaymentService {
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final Clock clock;
    private final br.com.espiritolabs.espirix.security.EmpresaAccess empresa;
    private final br.com.espiritolabs.espirix.repository.CompanyRepository companies;
    private final AuditLogRepository audit;

    @Transactional
    public TituloResponse pagar(Long id, PagamentoRequest request) {
        companies.findLockedById(empresa.id()).orElseThrow();
        Invoice i = locked(id);
        aplicar(i, request.valor(), request.dataPagamento(), request.observacao());
        return DtoMapper.titulo(i, LocalDate.now(clock));
    }
    @Transactional
    public List<TituloResponse> pagarLote(LotePagamentosRequest request) {
        companies.findLockedById(empresa.id()).orElseThrow();
        Set<Long> unique = new HashSet<>();
        for (ItemBaixaRequest item : request.pagamentos()) {
            if (!unique.add(item.tituloId())) throw BusinessException.badRequest("Titulo repetido no mesmo lote: " + item.tituloId());
        }
        // Ordem fixa de locks evita deadlocks entre lotes concorrentes.
        Map<Long, Invoice> locked = new HashMap<>();
        unique.stream().sorted().forEach(id -> locked.put(id, locked(id)));
        List<TituloResponse> result = new ArrayList<>();
        for (ItemBaixaRequest item : request.pagamentos()) {
            Invoice invoice = locked.get(item.tituloId());
            aplicar(invoice, item.valor(), item.dataPagamento(), request.observacao());
            result.add(DtoMapper.titulo(invoice, LocalDate.now(clock)));
        }
        return result;
    }
    @Transactional
    public List<TituloResponse> quitarLote(QuitarLoteRequest request) {
        companies.findLockedById(empresa.id()).orElseThrow();
        if (new HashSet<>(request.titulosIds()).size() != request.titulosIds().size())
            throw BusinessException.badRequest("IDs de titulos repetidos no lote");
        List<Invoice> locked = request.titulosIds().stream().sorted().map(this::locked).toList();
        List<TituloResponse> result = new ArrayList<>();
        for (Invoice invoice : locked) {
            if (invoice.getSaldoDevedor().signum() == 0)
                throw BusinessException.badRequest("Titulo ja quitado: " + invoice.getId());
            aplicar(invoice, invoice.getSaldoDevedor(), request.dataPagamento(), request.observacao());
            result.add(DtoMapper.titulo(invoice, LocalDate.now(clock)));
        }
        return result;
    }
    /** Estorno auditado e reversivel (o pagamento original permanece no historico). */
    @Transactional
    public TituloResponse estornar(Long tituloId, Long pagamentoId, String motivo) {
        if (motivo == null || motivo.isBlank() || motivo.trim().length() > 300)
            throw BusinessException.badRequest("Informe o motivo do estorno (ate 300 caracteres)");
        companies.findLockedById(empresa.id()).orElseThrow();
        Invoice invoice = locked(tituloId);
        Payment pagamento = payments.findByIdAndEmpresaIdAndTituloId(pagamentoId, empresa.id(), tituloId)
            .orElseThrow(() -> BusinessException.notFound("Pagamento nao encontrado"));
        if (pagamento.getOrigem() != OrigemPagamento.MANUAL)
            throw BusinessException.badRequest("Pagamentos importados nao podem ser estornados manualmente");
        if (pagamento.getEstornadoEm() != null)
            throw BusinessException.badRequest("Pagamento ja estornado");
        pagamento.setEstornadoEm(LocalDateTime.now(clock));
        pagamento.setEstornadoPor(empresa.principal().id());
        pagamento.setMotivoEstorno(motivo.trim());
        payments.save(pagamento);
        invoice.setValorPago(invoice.getValorPago().subtract(pagamento.getValor()));
        invoice.setDataPagamento(payments.findByEmpresaIdAndTituloIdOrderByDataPagamentoAscIdAsc(empresa.id(), tituloId)
            .stream().filter(p -> p.getEstornadoEm() == null)
            .map(Payment::getDataPagamento).max(LocalDate::compareTo).orElse(null));
        invoice.recalcular(LocalDate.now(clock));
        invoices.save(invoice);
        AuditLog log = new AuditLog();
        log.setEmpresaId(empresa.id());
        log.setUsuarioId(empresa.principal().id());
        log.setAcao("ESTORNO_PAGAMENTO");
        log.setDetalhes("titulo=" + tituloId + ";pagamento=" + pagamentoId
            + ";valor=" + pagamento.getValor() + ";motivo=" + motivo.trim());
        audit.save(log);
        return DtoMapper.titulo(invoice, LocalDate.now(clock));
    }
    private Invoice locked(Long id) {
        return invoices.findLockedByIdAndEmpresaId(id,empresa.id())
            .orElseThrow(() -> BusinessException.notFound("Titulo nao encontrado: " + id));
    }
    private void aplicar(Invoice invoice, BigDecimal valor, LocalDate date, String obs) {
        if (date == null || date.isAfter(LocalDate.now(clock)))
            throw BusinessException.badRequest("Data de pagamento fora do intervalo valido para o titulo " + invoice.getId());
        if (valor == null || valor.signum() <= 0 || FinanceUtils.dinheiro(valor).compareTo(invoice.getSaldoDevedor()) > 0)
            throw BusinessException.badRequest("Pagamento supera o saldo ou e nao positivo: titulo " + invoice.getId());
        valor = FinanceUtils.dinheiro(valor);
        Payment payment = new Payment();
        payment.setEmpresaId(empresa.id());
        payment.setTitulo(invoice);
        payment.setValor(valor);
        payment.setDataPagamento(date);
        payment.setOrigem(OrigemPagamento.MANUAL);
        payment.setUsuarioId(empresa.principal().id());
        payment.setObservacao(obs);
        payments.save(payment);
        invoice.setValorPago(invoice.getValorPago().add(valor));
        if (invoice.getDataPagamento() == null || invoice.getDataPagamento().isBefore(date)) invoice.setDataPagamento(date);
        invoice.recalcular(LocalDate.now(clock));
        invoices.save(invoice);
        AuditLog log = new AuditLog();
        log.setEmpresaId(empresa.id());
        log.setUsuarioId(empresa.principal().id());
        log.setAcao("PAGAMENTO_MANUAL");
        log.setDetalhes("titulo=" + invoice.getId() + ";valor=" + valor + ";data=" + date);
        audit.save(log);
    }
}
