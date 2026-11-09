package br.com.espiritolabs.espirix.importer;

import br.com.espiritolabs.espirix.domain.TipoOperacao;
import br.com.espiritolabs.espirix.util.FinanceUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Mapeamento tolerante a aliases de CABECALHO; valores continuam validados estritamente. */
public final class ReportRowMapper {
    private ReportRowMapper() { }

    private static final Map<String, String> ALIASES = new HashMap<>();
    static {
        alias("razao", "razao social", "razao social cliente", "cliente", "cliente fornecedor",
            "cliente ou fornecedor", "nome do cliente", "fornecedor", "sacado", "favorecido", "nome cliente", "nome fornecedor", "cliente fornecedor razao social");
        alias("fantasia", "nome fantasia", "fantasia", "nome comercial");
        alias("cnpj", "cnpj cpf", "cpf cnpj", "cpf", "cnpj", "documento cliente", "cnpj cpf cliente");
        alias("numero", "numero documento", "numero do documento", "n documento", "no documento",
            "num documento", "documento", "numero da nota", "numero nota", "numero nf", "nota fiscal",
            "nf", "numero do titulo", "numero titulo", "titulo", "n titulo", "numero", "n nota",
            "numero da nf", "n da nota", "numero da nf se", "documento nf");
        alias("parcela", "parcela", "numero parcela", "n parcela", "prestacao", "numero prestacao");
        alias("tipo", "tipo operacao", "operacao", "tipo titulo", "natureza", "conta", "contas a pagar receber");
        alias("emissao", "emissao", "data emissao", "dt emissao", "data da emissao", "data de emissao");
        alias("vencimento", "vencimento", "data vencimento", "dt vencimento", "data do vencimento", "data de vencimento");
        alias("pagamento", "pagamento", "data pagamento", "data baixa", "data recebimento", "dt pagamento", "dt baixa");
        alias("valor", "valor", "valor original", "valor titulo", "valor do titulo", "valor documento",
            "valor da nota", "valor total", "vlr original", "vlr titulo", "valor a receber", "valor a pagar",
            "vlr documento", "vlr total", "valor total do documento", "valor da conta", "valor liquido");
        alias("valorPago", "valor pago", "valor recebido", "valor baixado", "valor liquidado",
            "valor ja pago", "valor ja recebido", "vlr pago", "vlr baixado", "recebido");
        alias("saldo", "saldo", "saldo devedor", "saldo aberto", "saldo a receber", "saldo a pagar", "valor em aberto", "a receber");
        alias("status", "status", "situacao", "situacao titulo", "status titulo");
    }
    private static void alias(String canonical, String... synonyms) {
        for (String s : synonyms) ALIASES.put(FinanceUtils.norm(s), canonical);
    }
    public static String canonical(String key) { return ALIASES.get(FinanceUtils.norm(key)); }

    public static void validateHeaders(Collection<String> headers) {
        Set<String> actual = new HashSet<>();
        headers.forEach(h -> { String mapped = canonical(h); if (mapped != null) actual.add(mapped); });
        for (String required : List.of("razao", "numero", "emissao", "vencimento", "valor")) {
            if (!actual.contains(required)) throw new IllegalArgumentException(
                "Cabecalho do relatorio nao reconhecido: falta campo '" + required + "'. "
                + "Confira os aliases em ReportRowMapper ou adapte o layout OMIE.");
        }
    }
    public static ImportedInvoice map(Map<String, String> row) {
        String razao = req(row, "razao");
        String numero = FinanceUtils.documento(req(row, "numero"));
        String parcela = row.getOrDefault("parcela", "");
        parcela = parcela == null || parcela.isBlank() ? "UNICA" : FinanceUtils.documento(parcela);
        LocalDate emissao = FinanceUtils.parseDate(req(row, "emissao"));
        LocalDate vencimento = FinanceUtils.parseDate(req(row, "vencimento"));
        BigDecimal original = FinanceUtils.parseMoney(req(row, "valor"));
        if (original == null || original.signum() <= 0) throw new IllegalArgumentException("Valor original deve ser maior que zero");
        BigDecimal pago = FinanceUtils.parseMoney(row.get("valorPago"));
        if (pago == null) {
            String situation = FinanceUtils.norm(row.getOrDefault("status", ""));
            pago = situation.equals("pago") || situation.equals("paga")
                || situation.equals("liquidado") || situation.equals("quitado")
                || situation.equals("recebido") ? original : BigDecimal.ZERO.setScale(2);
        }
        if (pago.signum() < 0 || pago.compareTo(original) > 0)
            throw new IllegalArgumentException("Valor pago fora do limite do titulo " + numero);
        String rawType = FinanceUtils.norm(row.getOrDefault("tipo", ""));
        TipoOperacao tipo = rawType.contains("pagar") || rawType.contains("despesa")
                ? TipoOperacao.PAGAR : TipoOperacao.RECEBER;
        LocalDate pagamento = FinanceUtils.parseDate(row.get("pagamento"));
        if (pago.signum() > 0 && pagamento == null)
            throw new IllegalArgumentException("Titulo " + numero + " tem valor pago, mas falta data de pagamento");
        if (pagamento != null && pagamento.isBefore(emissao))
            throw new IllegalArgumentException("Pagamento anterior a emissao do titulo " + numero);
        return new ImportedInvoice(razao.trim(), blankToNull(row.get("fantasia")),
            FinanceUtils.digits(row.get("cnpj")), numero, parcela, tipo, emissao,
            vencimento, pagamento, original, FinanceUtils.dinheiro(pago));
    }
    public static Map<String, String> canonicalValues(Map<String, String> raw) {
        Map<String, String> result = new HashMap<>();
        raw.forEach((key, value) -> {
            String mapped = canonical(key);
            if (mapped != null && !result.containsKey(mapped)) result.put(mapped, value == null ? "" : value.trim());
        });
        return result;
    }
    private static String req(Map<String, String> values, String key) {
        String val = values.get(key);
        if (val == null || val.isBlank()) throw new IllegalArgumentException("Campo obrigatorio ausente: " + key);
        return val;
    }
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
