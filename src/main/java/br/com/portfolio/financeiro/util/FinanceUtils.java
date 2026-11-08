package br.com.portfolio.financeiro.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.util.Locale;

public final class FinanceUtils {
    private FinanceUtils() { }
    private static final DateTimeFormatter BR_DATE = new DateTimeFormatterBuilder()
        .appendPattern("dd/MM/uuuu").toFormatter().withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter SHORT_DATE = new DateTimeFormatterBuilder()
        .appendPattern("dd/MM/uu").toFormatter().withResolverStyle(ResolverStyle.STRICT);

    public static String norm(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
    public static String digits(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
    public static String clienteKey(String taxId, String nome) {
        String digits = digits(taxId);
        return digits.isBlank() ? "NOME:" + norm(nome) : "DOC:" + digits;
    }
    public static String documento(String numero) {
        if (numero == null || numero.isBlank()) throw new IllegalArgumentException("Numero do documento ausente");
        return numero.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
    public static BigDecimal dinheiro(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Valor monetario ausente");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    public static BigDecimal parseMoney(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim().replace("R$", "").replace(" ", "").replace("\u00a0", "");
        boolean negativeParen = value.startsWith("(") && value.endsWith(");");
        if (negativeParen) value = value.substring(1, value.length() - 1);
        int comma = value.lastIndexOf(',');
        int dot = value.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            if (comma > dot) value = value.replace(".", "").replace(',', '.');
            else value = value.replace(",", "");
        } else if (comma >= 0) {
            value = value.replace(".", "").replace(',', '.');
        } else if (dot >= 0 && value.substring(dot + 1).length() == 3 && value.indexOf('.') == dot) {
            // 1.234 isolado: assume agrupamento de milhares brasileiro.
            value = value.replace(".", "");
        }
        try {
            BigDecimal money = new BigDecimal(value);
            return dinheiro(negativeParen ? money.negate() : money);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor monetario invalido: " + raw);
        }
    }
    public static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.length() >= 10 && value.charAt(4) == '-') {
            return LocalDate.parse(value.substring(0, 10));
        }
        if (value.length() >= 10 && value.charAt(2) == '/') {
            return LocalDate.parse(value.substring(0, 10), BR_DATE);
        }
        if (value.length() == 8 && value.charAt(2) == '/') return LocalDate.parse(value, SHORT_DATE);
        throw new IllegalArgumentException("Data invalida: " + raw + " (use dd/MM/yyyy ou yyyy-MM-dd)");
    }
}
