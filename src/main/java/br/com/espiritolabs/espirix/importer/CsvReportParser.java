package br.com.espiritolabs.espirix.importer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

@Component
public class CsvReportParser implements ReportParser {
    @Override
    public List<ImportedInvoice> parse(byte[] bytes) throws IOException {
        String content = decode(bytes).replace("\uFEFF", "");
        if (content.isBlank()) throw new IllegalArgumentException("CSV vazio");
        char delimiter = detectDelimiter(content);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true).setTrim(true).get();
        List<ImportedInvoice> result = new ArrayList<>();
        try (CSVParser parser = format.parse(new StringReader(content))) {
            ReportRowMapper.validateHeaders(parser.getHeaderNames());
            for (CSVRecord record : parser) {
                if (record.stream().allMatch(String::isBlank)) continue;
                if (!record.isConsistent()) throw new IllegalArgumentException(
                    "Linha CSV " + record.getRecordNumber() + ": quantidade irregular de colunas");
                Map<String, String> row = new LinkedHashMap<>();
                parser.getHeaderNames().forEach(name -> row.put(name, record.get(name)));
                try {
                    result.add(ReportRowMapper.map(ReportRowMapper.canonicalValues(row)));
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("Linha CSV " + record.getRecordNumber() + ": " + ex.getMessage(), ex);
                }
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException("CSV sem titulos validos");
        return result;
    }
    private String decode(byte[] bytes) throws CharacterCodingException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        try { return decoder.decode(ByteBuffer.wrap(bytes)).toString(); }
        catch (CharacterCodingException e) { return Charset.forName("windows-1252").decode(ByteBuffer.wrap(bytes)).toString(); }
    }
    private char detectDelimiter(String content) {
        String first = content.lines().filter(s -> !s.isBlank()).findFirst().orElse("");
        char[] candidates = {';', ',', '\t'};
        char best = ';'; int max = -1;
        for (char c : candidates) {
            int matches = 0; boolean quoted = false;
            for (int i = 0; i < first.length(); i++) {
                char ch = first.charAt(i);
                if (ch == '"') quoted = !quoted;
                else if (ch == c && !quoted) matches++;
            }
            if (matches > max) { max = matches; best = c; }
        }
        if (max < 2) throw new IllegalArgumentException("Delimitador CSV nao identificado; use ; , ou tab");
        return best;
    }
}
