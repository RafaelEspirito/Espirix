package br.com.espiritolabs.espirix.importer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class CsvReportParser implements ReportParser {
    private static final DateTimeFormatter OMIE_US_DATE = new DateTimeFormatterBuilder()
        .appendPattern("M/d/uuuu").toFormatter(Locale.US).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    @Override
    public List<ImportedInvoice> parse(byte[] bytes) throws IOException {
        String content = decode(bytes).replace("\uFEFF", "");
        if (content.isBlank()) throw new IllegalArgumentException("CSV vazio");
        // O CSV "pivot" do OMIE comeca com um titulo de relatorio, antes do cabecalho real.
        // Remover apenas quando a segunda linha confirma o layout, sem afetar CSVs comuns.
        String[] firstLines = content.split("\\R", 3);
        if (firstLines.length >= 2
                && firstLines[0].strip().startsWith("Contas a Receber,")
                && firstLines[1].contains("Minha Empresa (Nome Fantasia)")
                && firstLines[1].contains("Valor da Conta")) {
            content = content.substring(content.indexOf('\n') + 1);
        }
        char delimiter = detectDelimiter(content);
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true).setTrim(true).get();
        List<ImportedInvoice> result = new ArrayList<>();
        try (CSVParser parser = format.parse(new StringReader(content))) {
            List<String> headers = parser.getHeaderNames();
            ReportRowMapper.validateHeaders(headers);
            boolean omiePivot = headers.contains("Minha Empresa (Nome Fantasia)")
                    && headers.contains("Valor da Conta") && headers.contains("A Receber");
            for (CSVRecord record : parser) {
                if (record.stream().allMatch(String::isBlank)) continue;
                if (!record.isConsistent()) throw new IllegalArgumentException(
                    "Linha CSV " + record.getRecordNumber() + ": quantidade irregular de colunas");
                // Linha de totalizacao: primeiras 21 colunas vazias e total monetario na 22a.
                if (omiePivot && isOmieTotal(record)) continue;
                Map<String, String> row = new LinkedHashMap<>();
                headers.forEach(name -> row.put(name, record.get(name)));
                try {
                    // O exportador OMIE pivot usa M/d/yyyy, nao dd/MM/yyyy.
                    // Converter SOMENTE esse layout evita inverter datas de outros CSVs.
                    if (omiePivot) {
                        row.put("Emissão", omieDateToBr(row.get("Emissão")));
                        row.put("Vencimento", omieDateToBr(row.get("Vencimento")));
                    }
                    result.add(ReportRowMapper.map(ReportRowMapper.canonicalValues(row)));
                } catch (RuntimeException ex) {
                    throw new IllegalArgumentException("Linha CSV " + record.getRecordNumber() + ": " + ex.getMessage(), ex);
                }
            }
        }
        if (result.isEmpty()) throw new IllegalArgumentException("CSV sem titulos validos");
        return result;
    }
    private static boolean isOmieTotal(CSVRecord record) {
        if (record.size() < 22 || record.get(21).isBlank()) return false;
        for (int i = 0; i < 21; i++) {
            if (!record.get(i).isBlank()) return false;
        }
        return true;
    }
    private static String omieDateToBr(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        try {
            return LocalDate.parse(raw.trim(), OMIE_US_DATE).format(BR_DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data OMIE invalida: " + raw + " (esperado M/d/yyyy)", ex);
        }
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
