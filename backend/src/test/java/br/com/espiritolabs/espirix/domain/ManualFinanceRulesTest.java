package br.com.espiritolabs.espirix.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ManualFinanceRulesTest {
    @Test void despesaPendenteApareceComoAVencer() {
        Invoice t = new Invoice();
        t.setOrigemArquivo(OrigemArquivo.MANUAL);
        t.setTipoOperacao(TipoOperacao.PAGAR);
        t.setDataVencimento(LocalDate.of(2026, 11, 20));
        t.setValorOriginal(new BigDecimal("500.00"));
        t.setValorPago(BigDecimal.ZERO);
        t.recalcular(LocalDate.of(2026, 11, 10));
        assertEquals(new BigDecimal("500.00"), t.getSaldoDevedor());
        assertEquals(StatusTitulo.A_VENCER, t.getStatus());
    }
    @Test void quitarEEstornarReabreConta() {
        Invoice t = new Invoice();
        t.setDataVencimento(LocalDate.of(2026, 11, 10));
        t.setValorOriginal(new BigDecimal("145.00"));
        t.setValorPago(new BigDecimal("145.00"));
        t.recalcular(LocalDate.of(2026, 11, 10));
        assertEquals(StatusTitulo.PAGA, t.getStatus());
        t.setValorPago(BigDecimal.ZERO);
        t.recalcular(LocalDate.of(2026, 11, 11));
        assertEquals(StatusTitulo.VENCIDA, t.getStatus());
        assertEquals(new BigDecimal("145.00"), t.getSaldoDevedor());
    }
    @Test void pagamentoAcimaDoOriginalNaoEPermitido() {
        Invoice t = new Invoice();
        t.setValorOriginal(new BigDecimal("100.00"));
        t.setValorPago(new BigDecimal("100.01"));
        assertThrows(IllegalArgumentException.class, () -> t.recalcular(LocalDate.of(2026,11,10)));
    }
}
