package br.com.espiritolabs.espirix.importer;

import br.com.espiritolabs.espirix.domain.TipoOperacao;
import org.junit.jupiter.api.Test;
import java.nio.charset.*;
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
    @Test void rejeitaCabecalhosDesconhecidos() {
        assertThatThrownBy(() -> parser.parse("col1;col2;col3\na;b;c".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Cabecalho");
    }
}
