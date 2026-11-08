package br.com.portfolio.financeiro.service;

import br.com.portfolio.financeiro.domain.*;
import br.com.portfolio.financeiro.dto.ApiDtos.*;
import br.com.portfolio.financeiro.exception.BusinessException;
import br.com.portfolio.financeiro.repository.*;
import br.com.portfolio.financeiro.util.FinanceUtils;
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

    @Transactional
    public TituloResponse pagar(Long id, PagamentoRequest request) {
        Invoice i = locked(id);
        aplicar(i, request.valor(), request.dataPagamento(), request.observacao());
        return DtoMapper.titulo(i, LocalDate.now(clock));
    }
    @Transactional
    public List<TituloResponse> pagarLote(LotePagamentosRequest request) {
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
    private Invoice locked(Long id) {
        return invoices.findLockedById(id)
            .orElseThrow(() -> BusinessException.notFound("Titulo nao encontrado: " + id));
    }
    private void aplicar(Invoice invoice, BigDecimal valor, LocalDate date, String obs) {
        if (date == null || date.isAfter(LocalDate.now(clock)) || date.isBefore(invoice.getDataEmissao()))
            throw BusinessException.badRequest("Data de pagamento fora do intervalo valido para o titulo " + invoice.getId());
        if (valor == null || valor.signum() <= 0 || FinanceUtils.dinheiro(valor).compareTo(invoice.getSaldoDevedor()) > 0)
            throw BusinessException.badRequest("Pagamento supera o saldo ou e nao positivo: titulo " + invoice.getId());
        valor = FinanceUtils.dinheiro(valor);
        Payment payment = new Payment();
        payment.setTitulo(invoice);
        payment.setValor(valor);
        payment.setDataPagamento(date);
        payment.setOrigem(OrigemPagamento.MANUAL);
        payment.setObservacao(obs);
        payments.save(payment);
        invoice.setValorPago(invoice.getValorPago().add(valor));
        if (invoice.getDataPagamento() == null || invoice.getDataPagamento().isBefore(date)) invoice.setDataPagamento(date);
        invoice.recalcular(LocalDate.now(clock));
        invoices.save(invoice);
    }
}
