package br.com.espiritolabs.espirix.service;

import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.dto.ApiDtos.ImportacaoResponse;
import br.com.espiritolabs.espirix.exception.BusinessException;
import br.com.espiritolabs.espirix.importer.*;
import br.com.espiritolabs.espirix.repository.*;
import br.com.espiritolabs.espirix.util.FinanceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class ImportService {
    private final CsvReportParser csv;
    private final PdfReportParser pdf;
    private final ClientRepository clients;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final ImportBatchRepository batches;
    private final Clock clock;
    private final br.com.espiritolabs.espirix.security.EmpresaAccess empresa;
    private final br.com.espiritolabs.espirix.repository.CompanyRepository companies;
    @Value("${app.importacao.max-registros:20000}") private int maxRows;

    @Transactional
    public ImportacaoResponse importar(String filename, OrigemArquivo source,
                                       byte[] bytes, String checksum, TipoOperacao tipoForcado) throws IOException {
        companies.findLockedById(empresa.id()).orElseThrow();
        List<ImportedInvoice> imported = (source == OrigemArquivo.CSV ? csv : pdf).parse(bytes);
        if (imported.size() > maxRows) throw BusinessException.badRequest("Arquivo excede " + maxRows + " titulos");
        ImportBatch batch = new ImportBatch();
        batch.setEmpresaId(empresa.id());
        batch.setNomeArquivo(filename);
        batch.setTipoArquivo(source);
        batch.setSha256(checksum);
        batch.setStatus(StatusImportacao.SUCESSO);
        batch.setMensagem("Importacao concluida. Atualizacoes de ERP nao estornam baixas anteriores.");
        batches.save(batch);

        int created = 0, updated = 0, ignored = 0;
        Set<String> seen = new HashSet<>();
        for (ImportedInvoice originalInput : imported) {
            ImportedInvoice input = tipoForcado == null ? originalInput : new ImportedInvoice(
                originalInput.razaoSocial(), originalInput.nomeFantasia(), originalInput.cnpjCpf(),
                originalInput.numeroNota(), originalInput.numeroParcela(), tipoForcado,
                originalInput.dataEmissao(), originalInput.dataVencimento(), originalInput.dataPagamento(),
                originalInput.valorOriginal(), originalInput.valorPago());
            String clientKey = FinanceUtils.clienteKey(input.cnpjCpf(), input.razaoSocial());
            String naturalKey = clientKey + "|" + input.numeroNota() + "|" + input.numeroParcela() + "|" + input.tipoOperacao();
            if (!seen.add(naturalKey))
                throw BusinessException.badRequest("Titulo repetido dentro do mesmo arquivo: " + input.numeroNota()
                    + ", parcela " + input.numeroParcela());
            Client client = clients.findByEmpresaIdAndChaveBusca(empresa.id(),clientKey).orElseGet(() -> {
                // Um cadastro antigo sem CNPJ pode ser reconciliado quando chega a identificacao fiscal.
                if (clientKey.startsWith("DOC:")) {
                    String oldKey = FinanceUtils.clienteKey(null, input.razaoSocial());
                    var old = clients.findByEmpresaIdAndChaveBusca(empresa.id(),oldKey);
                    if (old.isPresent() && (old.get().getCnpjCpf() == null || old.get().getCnpjCpf().isBlank())) {
                        Client migrated = old.get();
                        migrated.setChaveBusca(clientKey);
                        return migrated;
                    }
                }
                return newClient(input, clientKey);
            });
            boolean clientUpdated = updateClient(client, input);
            client = clients.save(client);
            Optional<Invoice> opt = invoices.findByEmpresaIdAndClienteIdAndNumeroNotaAndNumeroParcelaAndTipoOperacao(
                empresa.id(), client.getId(), input.numeroNota(), input.numeroParcela(), input.tipoOperacao());
            Invoice invoice;
            boolean changed = clientUpdated;
            if (opt.isEmpty()) {
                invoice = new Invoice();
                invoice.setEmpresaId(empresa.id());
                invoice.setCliente(client);
                invoice.setNumeroNota(input.numeroNota());
                invoice.setNumeroParcela(input.numeroParcela());
                invoice.setTipoOperacao(input.tipoOperacao());
                invoice.setValorPago(BigDecimal.ZERO.setScale(2));
                changed = true;
                created++;
            } else invoice = opt.get();
            if (invoice.getValorPago().compareTo(input.valorOriginal()) > 0)
                throw BusinessException.badRequest("Novo valor do titulo " + input.numeroNota()
                    + " e menor que valores ja liquidados");
            changed |= changeInvoiceFields(invoice, input, source);
            // Relatorio ERP representa um TOTAL PAGO acumulado. Importamos apenas o delta.
            // Nunca subtraimos pagamentos manuais/anteriores ao reimportar uma planilha antiga.
            BigDecimal delta = input.valorPago().subtract(invoice.getValorPago());
            if (delta.signum() > 0) {
                if (input.dataPagamento() == null || input.dataPagamento().isAfter(LocalDate.now(clock)))
                    throw BusinessException.badRequest("Data do pagamento importado invalida");
                Payment payment = new Payment();
                payment.setEmpresaId(empresa.id());
                payment.setTitulo(invoice);
                payment.setValor(delta);
                payment.setDataPagamento(input.dataPagamento());
                payment.setOrigem(OrigemPagamento.IMPORTACAO);
                payment.setImportacao(batch);
                payment.setObservacao("Ajuste cumulativo importado do ERP");
                // A identidade do titulo deve existir antes do pagamento.
                invoice.setValorPago(invoice.getValorPago().add(delta));
                invoice.setDataPagamento(maxDate(invoice.getDataPagamento(), input.dataPagamento()));
                changed = true;
                invoice.recalcular(LocalDate.now(clock));
                invoices.saveAndFlush(invoice);
                payments.save(payment);
            } else {
                invoice.recalcular(LocalDate.now(clock));
                if (changed) invoices.save(invoice);
            }
            if (opt.isPresent()) {
                if (changed) updated++;
                else ignored++;
            }
        }
        batch.setQuantidadeRegistrosNovos(created);
        batch.setQuantidadeAtualizados(updated);
        batch.setQuantidadeIgnorados(ignored);
        return DtoMapper.importacao(batches.saveAndFlush(batch));
    }

    private Client newClient(ImportedInvoice i, String key) {
        Client c = new Client();
        c.setEmpresaId(empresa.id());
        c.setChaveBusca(key);
        return c;
    }
    private boolean updateClient(Client c, ImportedInvoice i) {
        boolean change = !Objects.equals(c.getRazaoSocial(), i.razaoSocial());
        if (change) c.setRazaoSocial(i.razaoSocial());
        if (i.nomeFantasia() != null && !Objects.equals(c.getNomeFantasia(), i.nomeFantasia())) {
            c.setNomeFantasia(i.nomeFantasia()); change = true;
        }
        String digits = i.cnpjCpf();
        if (digits != null && !digits.isBlank() && !Objects.equals(c.getCnpjCpf(), digits)) {
            c.setCnpjCpf(digits); change = true;
        }
        return change;
    }
    private boolean changeInvoiceFields(Invoice i, ImportedInvoice input, OrigemArquivo source) {
        boolean changed = false;
        if (!Objects.equals(i.getDataEmissao(), input.dataEmissao())) { i.setDataEmissao(input.dataEmissao()); changed = true; }
        if (!Objects.equals(i.getDataVencimento(), input.dataVencimento())) { i.setDataVencimento(input.dataVencimento()); changed = true; }
        if (!Objects.equals(i.getValorOriginal(), input.valorOriginal())) { i.setValorOriginal(input.valorOriginal()); changed = true; }
        if (i.getOrigemArquivo() != source) { i.setOrigemArquivo(source); changed = true; }
        return changed;
    }
    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        return a == null ? b : (b != null && a.isBefore(b) ? b : a);
    }
    @Transactional(readOnly = true)
    public Page<ImportacaoResponse> historico(Pageable page) {
        return batches.findByEmpresaId(empresa.id(),page).map(DtoMapper::importacao);
    }
}
