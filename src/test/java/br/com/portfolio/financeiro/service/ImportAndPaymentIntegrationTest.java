package br.com.portfolio.financeiro.service;

import br.com.portfolio.financeiro.domain.*;
import br.com.portfolio.financeiro.dto.ApiDtos.*;
import br.com.portfolio.financeiro.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:integrationtests;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.task.scheduling.enabled=false"})
class ImportAndPaymentIntegrationTest {
    @Autowired private ImportService importer;
    @Autowired private PaymentService paymentService;
    @Autowired private InvoiceRepository invoices;
    @Autowired private PaymentRepository payments;
    @Autowired private DashboardService dashboard;
    @Autowired private Clock clock;

    private byte[] csv(String valorPago, String data) {
        LocalDate hoje = LocalDate.now(clock);
        String csv = "Razão Social;CNPJ/CPF;Número Documento;Data Emissão;Data Vencimento;Valor Original;Valor Pago;Data Pagamento\n"
            + "Cliente Teste;00000000000191;NF-TEST;" + hoje.minusDays(20).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            + ";" + hoje.plusDays(10).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            + ";1000,00;" + valorPago + ";" + data + "\n";
        return csv.getBytes(StandardCharsets.UTF_8);
    }
    @Test
    void importacaoIdempotenteBaixaParcialEReconciliacaoSemEstorno() throws Exception {
        var initial = csv("0,00", "");
        var first = importer.importar("primeiro.csv", OrigemArquivo.CSV, initial, "1", null);
        assertThat(first.quantidadeRegistrosNovos()).isEqualTo(1);
        var second = importer.importar("primeiro.csv", OrigemArquivo.CSV, initial, "1", null);
        assertThat(second.quantidadeIgnorados()).isEqualTo(1);
        Invoice invoice = invoices.findAll().getFirst();
        var pay = paymentService.pagar(invoice.getId(), new PagamentoRequest(
            new java.math.BigDecimal("150.00"), LocalDate.now(clock), "Parcial"));
        assertThat(pay.saldoDevedor()).isEqualByComparingTo("850.00");
        assertThat(pay.status()).isEqualTo(StatusTitulo.PAGA_PARCIAL);
        assertThat(dashboard.resumo(TipoOperacao.RECEBER).dividaAtiva()).isEqualByComparingTo("850.00");
        var oldAgain = importer.importar("antigo.csv", OrigemArquivo.CSV, initial, "1", null);
        assertThat(oldAgain.quantidadeIgnorados()).isEqualTo(1);
        String hojeBr = LocalDate.now(clock).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        var changed = importer.importar("atual.csv", OrigemArquivo.CSV, csv("200,00", hojeBr), "2", null);
        assertThat(changed.quantidadeAtualizados()).isEqualTo(1);
        Invoice refreshed = invoices.findById(invoice.getId()).orElseThrow();
        assertThat(refreshed.getSaldoDevedor()).isEqualByComparingTo("800.00");
        assertThat(payments.findByTituloIdOrderByDataPagamentoAscIdAsc(invoice.getId())).hasSize(2);
    }
}
