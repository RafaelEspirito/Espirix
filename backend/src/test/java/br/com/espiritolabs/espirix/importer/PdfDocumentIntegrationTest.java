package br.com.espiritolabs.espirix.importer;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.*;

class PdfDocumentIntegrationTest {
    @Test
    void leArquivoPdfTextualRealComColunas() throws Exception {
        byte[] bytes;
        try (PDDocument pdf = new PDDocument()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                String[][] lines = {
                    {"Razao Social", "Numero Documento", "Data Emissao", "Data Vencimento", "Valor Original"},
                    {"Cliente Teste", "NF-PDF-1", "01/10/2026", "30/11/2026", "1.200,00"}
                };
                float[] xs = {30, 195, 325, 420, 515};
                for (int r = 0; r < lines.length; r++) {
                    for (int col = 0; col < xs.length; col++) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
                        stream.newLineAtOffset(xs[col], 730 - 30 * r);
                        stream.showText(lines[r][col]);
                        stream.endText();
                    }
                }
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                pdf.save(output);
                bytes = output.toByteArray();
            }
        }
        var rows = new PdfReportParser().parse(bytes);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().numeroNota()).isEqualTo("NF-PDF-1");
    }
}
