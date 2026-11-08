package br.com.portfolio.financeiro.importer;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PdfReportParserTest {
    private final PdfReportParser parser = new PdfReportParser();

    @Test void interpretaTextoComColunasSeparadas() {
        String text = "Razão Social    Número Documento    Data Emissão    Data Vencimento    Valor Original\n"
                    + "Cliente Exemplo    NF-123    01/10/2026    30/11/2026    1.200,00\n";
        var rows = parser.parseTextTable(text);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().numeroNota()).isEqualTo("NF-123");
        assertThat(rows.getFirst().valorOriginal()).isEqualByComparingTo("1200");
    }
    @Test void rejeitaLayoutNaoReconhecido() {
        assertThatThrownBy(() -> parser.parseTextTable("Total geral de valores: 9999"))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Layout de PDF");
    }
}
