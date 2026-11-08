package br.com.espiritolabs.espirix.service;

import br.com.espiritolabs.espirix.domain.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

class InvoiceStatusTest {
    @Test void parcelaVencidaTambemPodeSerParcial() {
        Invoice i = new Invoice();
        i.setDataVencimento(LocalDate.of(2026, 1, 10));
        i.setValorOriginal(new BigDecimal("100.00"));
        i.setValorPago(new BigDecimal("30.00"));
        i.recalcular(LocalDate.of(2026, 11, 8));
        assertThat(i.getSaldoDevedor()).isEqualByComparingTo("70.00");
        assertThat(i.getStatus()).isEqualTo(StatusTitulo.PAGA_PARCIAL);
        assertThat(i.emAtraso(LocalDate.of(2026, 11, 8))).isTrue();
    }
    @Test void pagaIntegralmente() {
        Invoice i = new Invoice();
        i.setDataVencimento(LocalDate.of(2026, 11, 30));
        i.setValorOriginal(new BigDecimal("99.90"));
        i.setValorPago(new BigDecimal("99.90"));
        i.recalcular(LocalDate.of(2026, 11, 8));
        assertThat(i.getStatus()).isEqualTo(StatusTitulo.PAGA);
        assertThat(i.getSaldoDevedor()).isZero();
    }
}
