package br.com.espiritolabs.espirix.service;

import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.dto.ApiDtos.*;
import br.com.espiritolabs.espirix.exception.BusinessException;
import br.com.espiritolabs.espirix.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final ClientRepository clients;
    private final Clock clock;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private LocalDate hoje() { return LocalDate.now(clock); }
    private List<Invoice> titulos(TipoOperacao tipo, Long clienteId) {
        if (clienteId != null && !clients.existsById(clienteId)) throw BusinessException.notFound("Cliente nao encontrado");
        return clienteId == null ? invoices.findByTipoOperacao(tipo) : invoices.findByClienteIdAndTipoOperacao(clienteId, tipo);
    }
    private List<Payment> pagos(TipoOperacao tipo, Long clienteId) {
        return clienteId == null ? payments.findByTituloTipoOperacao(tipo)
            : payments.findByTituloClienteIdAndTituloTipoOperacao(clienteId, tipo);
    }
    private static BigDecimal sum(Collection<BigDecimal> valores) {
        return valores.stream().reduce(ZERO, BigDecimal::add);
    }
    public ResumoDashboard resumo(TipoOperacao tipo) {
        LocalDate hoje = hoje();
        YearMonth mes = YearMonth.from(hoje);
        List<Invoice> all = titulos(tipo, null);
        List<Invoice> abertas = all.stream().filter(i -> i.getSaldoDevedor().signum() > 0).toList();
        BigDecimal total = sum(abertas.stream().map(Invoice::getSaldoDevedor).toList());
        BigDecimal vencido = sum(abertas.stream().filter(i -> i.emAtraso(hoje)).map(Invoice::getSaldoDevedor).toList());
        BigDecimal d7 = sum(abertas.stream().filter(i -> entre(i, hoje, hoje.plusDays(7))).map(Invoice::getSaldoDevedor).toList());
        BigDecimal d15 = sum(abertas.stream().filter(i -> entre(i, hoje, hoje.plusDays(15))).map(Invoice::getSaldoDevedor).toList());
        BigDecimal d30 = sum(abertas.stream().filter(i -> entre(i, hoje, hoje.plusDays(30))).map(Invoice::getSaldoDevedor).toList());
        BigDecimal quitados = sum(all.stream().filter(i -> i.getSaldoDevedor().signum() == 0)
            .filter(i -> i.getDataPagamento() != null && YearMonth.from(i.getDataPagamento()).equals(mes))
            .map(Invoice::getValorOriginal).toList());
        BigDecimal recebido = sum(pagos(tipo, null).stream()
            .filter(p -> YearMonth.from(p.getDataPagamento()).equals(mes)).map(Payment::getValor).toList());
        return new ResumoDashboard(tipo, hoje, total, vencido, d7, d15, d30,
            quitados, recebido, abertas.size());
    }
    private static boolean entre(Invoice i, LocalDate begin, LocalDate end) {
        return !i.getDataVencimento().isBefore(begin) && !i.getDataVencimento().isAfter(end);
    }
    public List<FaixaPrevisao> previsao(TipoOperacao tipo) {
        LocalDate hoje = hoje();
        List<Invoice> list = titulos(tipo, null).stream().filter(i -> i.getSaldoDevedor().signum() > 0)
            .filter(i -> !i.getDataVencimento().isBefore(hoje)).toList();
        List<FaixaPrevisao> buckets = new ArrayList<>();
        List<String> labels = List.of("0-7 dias", "8-15 dias", "16-30 dias", "31+ dias");
        for (int bucket = 0; bucket < 4; bucket++) {
            final int index = bucket;
            List<Invoice> matches = list.stream().filter(i -> {
                long days = java.time.temporal.ChronoUnit.DAYS.between(hoje, i.getDataVencimento());
                return index == 0 ? days <= 7 : index == 1 ? days >= 8 && days <= 15
                    : index == 2 ? days >= 16 && days <= 30 : days > 30;
            }).toList();
            buckets.add(new FaixaPrevisao(labels.get(bucket),
                sum(matches.stream().map(Invoice::getSaldoDevedor).toList()), matches.size()));
        }
        return buckets;
    }
    public List<Devedor> topDevedores(TipoOperacao tipo, int limite) {
        if (limite < 1 || limite > 100) throw BusinessException.badRequest("Limite deve estar entre 1 e 100");
        return titulos(tipo, null).stream().filter(i -> i.getSaldoDevedor().signum() > 0)
            .collect(Collectors.groupingBy(i -> i.getCliente().getId())).values().stream()
            .map(group -> {
                Client c = group.getFirst().getCliente();
                return new Devedor(c.getId(), c.getNomeFantasia(), c.getRazaoSocial(),
                    sum(group.stream().map(Invoice::getSaldoDevedor).toList()), group.size());
            })
            .sorted(Comparator.comparing(Devedor::saldoDevedor).reversed())
            .limit(limite).toList();
    }
    public List<PontoEvolucao> evolucao(TipoOperacao tipo, Long clienteId, int meses) {
        if (meses < 1 || meses > 60) throw BusinessException.badRequest("Meses deve estar entre 1 e 60");
        List<Invoice> all = titulos(tipo, clienteId);
        List<Payment> allPayments = pagos(tipo, clienteId);
        YearMonth first = YearMonth.from(hoje()).minusMonths(meses - 1L);
        LocalDate start = first.atDay(1);
        // Saldo anterior ao recorte: inclui documentos emitidos e abatimentos anteriores.
        BigDecimal saldo = sum(all.stream().filter(i -> i.getDataEmissao().isBefore(start))
            .map(Invoice::getValorOriginal).toList()).subtract(sum(allPayments.stream()
            .filter(p -> p.getDataPagamento().isBefore(start)).map(Payment::getValor).toList()));
        List<PontoEvolucao> result = new ArrayList<>();
        for (int offset = 0; offset < meses; offset++) {
            YearMonth month = first.plusMonths(offset);
            BigDecimal novas = sum(all.stream().filter(i -> YearMonth.from(i.getDataEmissao()).equals(month))
                .map(Invoice::getValorOriginal).toList());
            BigDecimal recebimentos = sum(allPayments.stream().filter(p -> YearMonth.from(p.getDataPagamento()).equals(month))
                .map(Payment::getValor).toList());
            saldo = saldo.add(novas).subtract(recebimentos);
            result.add(new PontoEvolucao(month.toString(), novas, recebimentos, saldo));
        }
        return result;
    }
    public List<ConsolidadoCliente> consolidado(String busca, LocalDate emissaoDe, LocalDate emissaoAte,
                  LocalDate vencimentoDe, LocalDate vencimentoAte, StatusTitulo status, TipoOperacao tipo) {
        InvoiceFilters.Filtro filtro = new InvoiceFilters.Filtro(busca, emissaoDe, emissaoAte,
            vencimentoDe, vencimentoAte, status, tipo, null);
        List<Invoice> filtered = invoices.findAll(InvoiceFilters.build(filtro, hoje()));
        return filtered.stream().collect(Collectors.groupingBy(i -> i.getCliente().getId())).values().stream()
            .map(group -> {
                Client client = group.getFirst().getCliente();
                BigDecimal original = sum(group.stream().map(Invoice::getValorOriginal).toList());
                BigDecimal paid = sum(group.stream().map(Invoice::getValorPago).toList());
                BigDecimal due = sum(group.stream().map(Invoice::getSaldoDevedor).toList());
                long open = group.stream().filter(i -> i.getSaldoDevedor().signum() > 0).count();
                BigDecimal pct = original.signum() == 0 ? ZERO
                    : paid.multiply(BigDecimal.valueOf(100)).divide(original, 2, RoundingMode.HALF_UP);
                return new ConsolidadoCliente(client.getId(), client.getRazaoSocial(),
                    client.getNomeFantasia(), open, original, paid, due, pct);
            }).sorted(Comparator.comparing(ConsolidadoCliente::saldoDevedor).reversed()).toList();
    }
}
