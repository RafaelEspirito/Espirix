package br.com.espiritolabs.espirix.controller;

import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.dto.ApiDtos.*;
import br.com.espiritolabs.espirix.exception.BusinessException;
import br.com.espiritolabs.espirix.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api/titulos") @RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService service;
    private final PaymentService paymentService;

    @GetMapping
    public Page<TituloResponse> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate emissaoDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate emissaoAte,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimentoDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimentoAte,
            @RequestParam(required = false) StatusTitulo status,
            @RequestParam(defaultValue = "RECEBER") TipoOperacao tipoOperacao,
            @RequestParam(required = false) Boolean somenteAbertos,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) throw BusinessException.badRequest("Paginacao invalida");
        var filtro = new InvoiceFilters.Filtro(busca, emissaoDe, emissaoAte, vencimentoDe,
            vencimentoAte, status, tipoOperacao, somenteAbertos);
        return service.listar(filtro, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dataVencimento", "id")));
    }
    @GetMapping("/{id}")
    public TituloResponse buscar(@PathVariable Long id) { return service.buscar(id); }
    @GetMapping("/{id}/pagamentos")
    public List<PagamentoResponse> historico(@PathVariable Long id) { return service.historico(id); }
    @PostMapping("/{id}/pagamentos")
    public TituloResponse pagar(@PathVariable Long id, @Valid @RequestBody PagamentoRequest request) {
        return paymentService.pagar(id, request);
    }
    @PostMapping("/{id}/pagamentos/{pagamentoId}/estornar")
    public TituloResponse estornar(@PathVariable Long id, @PathVariable Long pagamentoId,
            @Valid @RequestBody EstornoRequest request) {
        return paymentService.estornar(id, pagamentoId, request.motivo());
    }
    @PostMapping("/pagamentos/lote")
    public List<TituloResponse> pagarLote(@Valid @RequestBody LotePagamentosRequest request) {
        return paymentService.pagarLote(request);
    }
    @PostMapping("/quitar-lote")
    public List<TituloResponse> quitarLote(@Valid @RequestBody QuitarLoteRequest request) {
        return paymentService.quitarLote(request);
    }
}
