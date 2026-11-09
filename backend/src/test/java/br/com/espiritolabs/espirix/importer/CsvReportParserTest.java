package br.com.espiritolabs.espirix.importer;

import br.com.espiritolabs.espirix.domain.TipoOperacao;
import org.junit.jupiter.api.Test;
import java.nio.charset.*;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class CsvReportParserTest {
    private final CsvReportParser parser = new CsvReportParser();
    private final String sample = "Razão Social;Número Documento;Data Emissão;Data Vencimento;Valor Original;Valor Pago;Data Pagamento\n"
            + "Cliente Alfa;NF-1;01/10/2026;30/11/2026;1.234,56;200,00;02/11/2026\n";

    @Test void parseValoresBrasileirosECabecalhosAcentuados() throws Exception {
        var list = parser.parse(sample.getBytes(StandardCharsets.UTF_8));
        assertThat(list).hasSize(1);
        assertThat(list.getFirst().valorOriginal()).isEqualByComparingTo("1234.56");
        assertThat(list.getFirst().valorPago()).isEqualByComparingTo("200.00");
        assertThat(list.getFirst().tipoOperacao()).isEqualTo(TipoOperacao.RECEBER);
    }
    @Test void suportaWindows1252() throws Exception {
        var list = parser.parse(sample.getBytes(Charset.forName("windows-1252")));
        assertThat(list).hasSize(1);
    }

    @Test void aceitaCsvPivotOmieComTituloTotalEDataAmericana() throws Exception {
        String title = "Contas a Receber" + ",".repeat(30) + "\n";
        String headers = String.join(",", java.util.List.of(
            "Minha Empresa (Nome Fantasia)", "Minha Empresa (Razão Social)", "Minha Empresa (CNPJ)",
            "Previsão", "CNPJ/CPF", "Razão Social", "Tags", "Emissão", "Vencimento", "Registro",
            "Categoria", "Conta Corrente", "Nota Fiscal", "Parcela", "Documento", "Número", "Boleto",
            "Pedido de Venda", "Vendedor", "Projeto", "Origem", "Valor da Conta", "Valor PIS",
            "Valor COFINS", "Valor CSLL", "Valor IR", "Valor ISS", "Valor INSS", "Valor Líquido",
            "Recebido", "A Receber")) + "\n";
        String total = ",".repeat(21) + "\"R$  1,350.00\"" + ",".repeat(9) + "\n";
        String invoice = String.join(",", java.util.List.of(
            "Empresa Teste", "Empresa Teste Ltda", "00.000.000/0001-00", "11/13/2026",
            "00.000.000/0001-00", "Cliente Exemplo", "Cliente", "11/5/2026", "11/13/2026",
            "11/5/2026", "Clientes - Vendas a Faturar", "Caixa Geral", "00000001", "001/001",
            "Outros", "N/D", "N/D", "123", "Vendedor Exemplo", "N/D", "Conta a Receber",
            "\"R$  1,350.00\"", "R$  0.00", "R$  0.00", "R$  0.00", "R$  0.00",
            "R$  0.00", "R$  0.00", "\"R$  1,350.00\"", "R$  0.00", "\"R$  1,350.00\"")) + "\n";
        var result = parser.parse((title + headers + total + invoice).getBytes(StandardCharsets.UTF_8));
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().numeroNota()).isEqualTo("00000001");
        assertThat(result.getFirst().dataEmissao()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(result.getFirst().dataVencimento()).isEqualTo(LocalDate.of(2026, 11, 13));
        assertThat(result.getFirst().valorOriginal()).isEqualByComparingTo("1350.00");
        assertThat(result.getFirst().valorPago()).isEqualByComparingTo("0.00");
    }
    @Test void rejeitaCabecalhosDesconhecidos() {
        assertThatThrownBy(() -> parser.parse("col1;col2;col3\na;b;c".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cabecalho");
    }
}
