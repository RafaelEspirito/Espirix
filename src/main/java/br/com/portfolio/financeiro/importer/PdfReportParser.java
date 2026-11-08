package br.com.portfolio.financeiro.importer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

/**
 * PDFBox extrai texto, nao tabelas semanticamente. Este adaptador suporta PDFs
 * textuais com colunas alinhadas por ao menos 2 espacos na extracao.
 * PDFs digitalizados ou outro layout devem ser rejeitados, nunca importados de modo parcial.
 */
@Component
public class PdfReportParser implements ReportParser {
    @Override
    public List<ImportedInvoice> parse(byte[] bytes) throws IOException {
        if (bytes.length < 5 || !new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
            throw new IllegalArgumentException("Arquivo nao possui assinatura PDF valida");
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) throw new IllegalArgumentException("PDF protegido por senha nao suportado");
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String extracted = stripper.getText(document);
            if (extracted == null || extracted.isBlank())
                throw new IllegalArgumentException("PDF sem texto selecionavel. OCR nao incluido nesta etapa");
            try { return parseTextTable(extracted); }
            catch (IllegalArgumentException firstPass) {
                // Se o extractor plano nao preserva espacos suficientes, tenta coordenadas X/Y.
                String reconstructed = new PdfGeometryExtractor().reconstruct(document);
                try { return parseTextTable(reconstructed); }
                catch (IllegalArgumentException secondPass) {
                    secondPass.addSuppressed(firstPass);
                    throw secondPass;
                }
            }
        }
    }

    /** Exposto para teste de layouts de PDF extraidos; ajuste o adaptador para cada modelo OMIE. */
    public List<ImportedInvoice> parseTextTable(String text) {
        List<ImportedInvoice> rows = new ArrayList<>();
        List<String> headers = null;
        for (String line : text.split("\\R")) {
            if (line.isBlank()) continue;
            List<String> cols = Arrays.stream(line.trim().split("\\s{2,}"))
                    .map(String::trim).toList();
            if (cols.size() < 5) continue;
            // Cada pagina pode repetir o cabecalho; cabecalho so e aceito com aliases obrigatorios.
            if (isHeader(cols)) { headers = cols; continue; }
            if (headers == null || cols.size() != headers.size()) continue;
            Map<String, String> values = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) values.put(headers.get(i), cols.get(i));
            try {
                rows.add(ReportRowMapper.map(ReportRowMapper.canonicalValues(values)));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Linha de PDF invalida: " + ex.getMessage(), ex);
            }
        }
        if (rows.isEmpty()) throw new IllegalArgumentException(
            "Layout de PDF nao reconhecido. Exporte CSV do OMIE ou adapte PdfReportParser ao relatorio textual real. "
            + "O PDF precisa de cabecalho e colunas alinhadas; nenhum registro foi gravado.");
        return rows;
    }
    private boolean isHeader(List<String> cols) {
        try { ReportRowMapper.validateHeaders(cols); return true; }
        catch (IllegalArgumentException e) { return false; }
    }
}
