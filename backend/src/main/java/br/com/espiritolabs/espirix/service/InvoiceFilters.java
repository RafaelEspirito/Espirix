package br.com.espiritolabs.espirix.service;

import br.com.espiritolabs.espirix.domain.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.ArrayList;

public final class InvoiceFilters {
    private InvoiceFilters() { }
    public record Filtro(String busca, LocalDate emissaoDe, LocalDate emissaoAte,
                         LocalDate vencimentoDe, LocalDate vencimentoAte,
                         StatusTitulo status, TipoOperacao tipoOperacao, Boolean somenteAbertos) { }

    public static Specification<Invoice> build(Filtro f, LocalDate hoje, Long empresaId) {
        if (f.emissaoDe() != null && f.emissaoAte() != null && f.emissaoDe().isAfter(f.emissaoAte()))
            throw new IllegalArgumentException("Periodo de emissao invertido");
        if (f.vencimentoDe() != null && f.vencimentoAte() != null && f.vencimentoDe().isAfter(f.vencimentoAte()))
            throw new IllegalArgumentException("Periodo de vencimento invertido");
        return (root, query, cb) -> {
            var tests = new ArrayList<Predicate>();
            tests.add(cb.equal(root.get("empresaId"), empresaId));
            if (f.tipoOperacao() != null) tests.add(cb.equal(root.get("tipoOperacao"), f.tipoOperacao()));
            if (f.busca() != null && !f.busca().isBlank()) {
                var client = root.join("cliente");
                String term = "%" + f.busca().trim().toLowerCase(java.util.Locale.ROOT) + "%";
                tests.add(cb.or(cb.like(cb.lower(client.get("razaoSocial")), term),
                    cb.like(cb.lower(client.get("nomeFantasia")), term),
                    cb.like(cb.lower(root.get("numeroNota")), term),
                    cb.like(cb.lower(root.get("descricao")), term)));
            }
            if (f.emissaoDe() != null) tests.add(cb.greaterThanOrEqualTo(root.get("dataEmissao"), f.emissaoDe()));
            if (f.emissaoAte() != null) tests.add(cb.lessThanOrEqualTo(root.get("dataEmissao"), f.emissaoAte()));
            if (f.vencimentoDe() != null) tests.add(cb.greaterThanOrEqualTo(root.get("dataVencimento"), f.vencimentoDe()));
            if (f.vencimentoAte() != null) tests.add(cb.lessThanOrEqualTo(root.get("dataVencimento"), f.vencimentoAte()));
            if (Boolean.TRUE.equals(f.somenteAbertos())) tests.add(cb.greaterThan(root.get("saldoDevedor"), java.math.BigDecimal.ZERO));
            if (f.status() != null) {
                // VENCIDA e A_VENCER sao filtros de prazo, incluem parcelas pagas parcialmente.
                // PAGA_PARCIAL e um estado de liquidacao; pode se sobrepor a VENCIDA.
                switch (f.status()) {
                    case PAGA -> tests.add(cb.equal(root.get("saldoDevedor"), java.math.BigDecimal.ZERO));
                    case PAGA_PARCIAL -> {
                        tests.add(cb.greaterThan(root.get("saldoDevedor"), java.math.BigDecimal.ZERO));
                        tests.add(cb.greaterThan(root.get("valorPago"), java.math.BigDecimal.ZERO));
                    }
                    case VENCIDA -> {
                        tests.add(cb.greaterThan(root.get("saldoDevedor"), java.math.BigDecimal.ZERO));
                        tests.add(cb.lessThan(root.get("dataVencimento"), hoje));
                    }
                    case A_VENCER -> {
                        tests.add(cb.greaterThan(root.get("saldoDevedor"), java.math.BigDecimal.ZERO));
                        tests.add(cb.greaterThanOrEqualTo(root.get("dataVencimento"), hoje));
                    }
                }
            }
            return cb.and(tests.toArray(Predicate[]::new));
        };
    }
}
