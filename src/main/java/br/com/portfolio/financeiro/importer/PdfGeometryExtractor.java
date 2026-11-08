package br.com.portfolio.financeiro.importer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import java.io.IOException;
import java.util.*;

/**
 * Segunda estrategia de leitura: reconstrucao de linhas e espacos usando posicoes X/Y.
 * Os valores de tolerancia sao heuristicas, nao um substituto para mapear cada modelo de PDF.
 */
final class PdfGeometryExtractor extends PDFTextStripper {
    private final Map<Integer, List<TextPosition>> glyphs = new LinkedHashMap<>();
    PdfGeometryExtractor() throws IOException { setSortByPosition(true); }

    @Override
    protected void processTextPosition(TextPosition position) {
        glyphs.computeIfAbsent(getCurrentPageNo(), page -> new ArrayList<>()).add(position);
        super.processTextPosition(position);
    }
    String reconstruct(PDDocument document) throws IOException {
        getText(document);
        StringBuilder output = new StringBuilder();
        for (List<TextPosition> pagePositions : glyphs.values()) {
            pagePositions.sort(Comparator.comparingDouble(TextPosition::getYDirAdj)
                .thenComparingDouble(TextPosition::getXDirAdj));
            List<List<TextPosition>> lines = new ArrayList<>();
            for (TextPosition p : pagePositions) {
                if (lines.isEmpty() || Math.abs(lines.getLast().getFirst().getYDirAdj() - p.getYDirAdj()) > 2.5f)
                    lines.add(new ArrayList<>());
                lines.getLast().add(p);
            }
            for (List<TextPosition> line : lines) {
                line.sort(Comparator.comparingDouble(TextPosition::getXDirAdj));
                StringBuilder sb = new StringBuilder();
                float right = Float.NaN;
                TextPosition prev = null;
                for (TextPosition p : line) {
                    String word = p.getUnicode();
                    if (word == null || word.isBlank()) continue;
                    if (prev != null) {
                        float gap = p.getXDirAdj() - right;
                        float space = Math.max(prev.getWidthOfSpace(), 3.0f);
                        if (gap > Math.max(11.0f, space * 2.8f)) sb.append("    ");
                        else if (gap > space * 0.5f) sb.append(' ');
                    }
                    sb.append(word);
                    right = p.getXDirAdj() + p.getWidthDirAdj();
                    prev = p;
                }
                if (!sb.isEmpty()) output.append(sb).append('\n');
            }
        }
        return output.toString();
    }
}
