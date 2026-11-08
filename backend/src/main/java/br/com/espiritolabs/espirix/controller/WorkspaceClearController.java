package br.com.espiritolabs.espirix.controller;
import br.com.espiritolabs.espirix.service.WorkspaceClearService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/limpeza") @RequiredArgsConstructor
public class WorkspaceClearController {
    private final WorkspaceClearService service;
    @GetMapping("/preview") public WorkspaceClearService.Preview preview(){return service.preview();}
    @PostMapping("/executar") public WorkspaceClearService.Preview limpar(@RequestBody WorkspaceClearService.Pedido p){return service.limpar(p);}
}
