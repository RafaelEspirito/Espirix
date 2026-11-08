package br.com.portfolio.financeiro.controller;

import br.com.portfolio.financeiro.domain.TipoOperacao;
import br.com.portfolio.financeiro.dto.ApiDtos.*;
import br.com.portfolio.financeiro.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/dashboard") @RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping("/resumo")
    public ResumoDashboard resumo(@RequestParam(defaultValue = "RECEBER") TipoOperacao tipoOperacao) {
        return service.resumo(tipoOperacao);
    }
    @GetMapping("/evolucao")
    public List<PontoEvolucao> evolucao(@RequestParam(defaultValue = "12") int meses,
            @RequestParam(defaultValue = "RECEBER") TipoOperacao tipoOperacao) {
        return service.evolucao(tipoOperacao, null, meses);
    }
    @GetMapping("/previsao")
    public List<FaixaPrevisao> previsao(@RequestParam(defaultValue = "RECEBER") TipoOperacao tipoOperacao) {
        return service.previsao(tipoOperacao);
    }
    @GetMapping("/top-devedores")
    public List<Devedor> devedores(@RequestParam(defaultValue = "5") int limite,
            @RequestParam(defaultValue = "RECEBER") TipoOperacao tipoOperacao) {
        return service.topDevedores(tipoOperacao, limite);
    }
}
