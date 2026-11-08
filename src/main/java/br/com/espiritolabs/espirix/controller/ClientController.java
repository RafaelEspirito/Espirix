package br.com.espiritolabs.espirix.controller;

import br.com.espiritolabs.espirix.domain.TipoOperacao;
import br.com.espiritolabs.espirix.domain.StatusTitulo;
import br.com.espiritolabs.espirix.dto.ApiDtos.*;
import br.com.espiritolabs.espirix.exception.BusinessException;
import br.com.espiritolabs.espirix.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/clientes") @RequiredArgsConstructor
public class ClientController {
    private final InvoiceService invoices;
    private final DashboardService dashboard;

    @GetMapping
    public Page<ClienteResponse> listar(@RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw BusinessException.badRequest("Paginacao invalida");
        return invoices.clientes(busca, PageRequest.of(page, size, Sort.by("razaoSocial")));
    }
    @GetMapping("/consolidado")
    public List<ConsolidadoCliente> consolidado(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate emissaoDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate emissaoAte,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimentoDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimentoAte,
            @RequestParam(required = false) StatusTitulo status,
            @RequestParam(defaultValue = "RECEBER") TipoOperacao tipoOperacao) {
        return dashboard.consolidado(busca, emissaoDe, emissaoAte, vencimentoDe,
                vencimentoAte, status, tipoOperacao);
    }
    @GetMapping("/{id}")
    public ClienteResponse buscar(@PathVariable Long id) { return invoices.cliente(id); }
    @GetMapping("/{id}/evolucao")
    public List<PontoEvolucao> evolucao(@PathVariable Long id,
            @RequestParam(defaultValue = "12") int meses,
            @RequestParam(defaultValue = "RECEBER") TipoOperacao tipoOperacao) {
        return dashboard.evolucao(tipoOperacao, id, meses);
    }
}
