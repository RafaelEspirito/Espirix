package br.com.espiritolabs.espirix.controller;

import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.dto.ApiDtos.TituloResponse;
import br.com.espiritolabs.espirix.exception.BusinessException;
import br.com.espiritolabs.espirix.repository.*;
import br.com.espiritolabs.espirix.security.EmpresaAccess;
import br.com.espiritolabs.espirix.service.DtoMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@RestController @RequestMapping("/api/despesas") @RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseCategoryRepository categorias;
    private final InvoiceRepository titulos;
    private final ClientRepository fornecedores;
    private final PaymentRepository pagamentos;
    private final AuditLogRepository auditoria;
    private final EmpresaAccess acesso;
    private final Clock clock;

    public record CategoriaRequest(@NotBlank @Size(max=100) String nome,
                                   @NotNull ExpenseCategory.Type tipo) {}
    public record CategoriaResponse(Long id, String nome, ExpenseCategory.Type tipo, boolean ativa) {}
    public record NovaDespesaRequest(@NotNull Long fornecedorId, @NotNull Long categoriaId,
            @NotBlank @Size(max=200) String descricao,
            @NotNull @PastOrPresent LocalDate dataEmissao,
            @NotNull LocalDate dataVencimento,
            @NotNull @DecimalMin("0.01") @Digits(integer=17, fraction=2) BigDecimal valor,
            @Size(max=300) String observacao) {}
    public record CategoriaResumo(String nome, BigDecimal totalPagoMes) {}

    private static String chave(String valor) {
        return valor.strip().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
    @GetMapping("/categorias") @Transactional(readOnly=true)
    public List<CategoriaResponse> categorias() {
        return categorias.findByEmpresaIdOrderByNomeAsc(acesso.id()).stream()
            .map(c -> new CategoriaResponse(c.getId(),c.getNome(),c.getTipo(),c.isAtiva())).toList();
    }
    @PostMapping("/categorias") @Transactional
    public CategoriaResponse criarCategoria(@Valid @RequestBody CategoriaRequest request) {
        String nome = request.nome().strip().replaceAll("\\s+", " ");
        if (nome.isBlank()) throw BusinessException.badRequest("Nome da categoria obrigatorio");
        if (categorias.existsByEmpresaIdAndChave(acesso.id(), chave(nome)))
            throw BusinessException.badRequest("Categoria ja cadastrada nesta empresa");
        ExpenseCategory c = new ExpenseCategory();
        c.setEmpresaId(acesso.id()); c.setNome(nome); c.setChave(chave(nome)); c.setTipo(request.tipo());
        categorias.saveAndFlush(c);
        registrar("CATEGORIA_DESPESA", "categoria=" + c.getId());
        return new CategoriaResponse(c.getId(),c.getNome(),c.getTipo(),c.isAtiva());
    }
    @PostMapping @Transactional
    public TituloResponse criar(@Valid @RequestBody NovaDespesaRequest request) {
        if (request.dataVencimento().isBefore(request.dataEmissao()))
            throw BusinessException.badRequest("Vencimento nao pode ser anterior a emissao");
        Client fornecedor = fornecedores.findByIdAndEmpresaId(request.fornecedorId(), acesso.id())
            .orElseThrow(() -> BusinessException.notFound("Fornecedor nao encontrado nesta empresa"));
        if (!fornecedor.isFornecedor())
            throw BusinessException.badRequest("Cadastro nao marcado como fornecedor");
        ExpenseCategory categoria = categorias.findByIdAndEmpresaId(request.categoriaId(), acesso.id())
            .orElseThrow(() -> BusinessException.notFound("Categoria nao encontrada nesta empresa"));
        if (!categoria.isAtiva()) throw BusinessException.badRequest("Categoria desativada");
        Invoice t = new Invoice();
        t.setEmpresaId(acesso.id());
        t.setCliente(fornecedor); t.setCategoria(categoria);
        t.setDescricao(request.descricao().strip()); t.setObservacao(request.observacao());
        // Chave interna nao depende da descricao e impede colisao com notas importadas.
        t.setNumeroNota("DESP-" + UUID.randomUUID().toString().replace("-", "").substring(0,20).toUpperCase(Locale.ROOT));
        t.setNumeroParcela("UNICA");
        t.setTipoOperacao(TipoOperacao.PAGAR);
        t.setDataEmissao(request.dataEmissao());
        t.setDataVencimento(request.dataVencimento());
        t.setValorOriginal(request.valor().setScale(2));
        t.setValorPago(BigDecimal.ZERO.setScale(2));
        t.setOrigemArquivo(OrigemArquivo.MANUAL);
        t.recalcular(LocalDate.now(clock));
        titulos.saveAndFlush(t);
        registrar("LANCAMENTO_DESPESA", "titulo=" + t.getId() + ";categoria=" + categoria.getId()
            + ";valor=" + t.getValorOriginal());
        return DtoMapper.titulo(t,LocalDate.now(clock));
    }
    @GetMapping("/categorias/resumo") @Transactional(readOnly=true)
    public List<CategoriaResumo> resumoCategorias() {
        YearMonth mes = YearMonth.from(LocalDate.now(clock));
        Map<String,BigDecimal> resumo = new HashMap<>();
        for (Payment pagamento : pagamentos.findByEmpresaIdAndTituloTipoOperacaoAndEstornadoEmIsNull(acesso.id(),TipoOperacao.PAGAR)) {
            Invoice titulo = pagamento.getTitulo();
            if (titulo.getOrigemArquivo() != OrigemArquivo.MANUAL || titulo.getCategoria() == null
                || !YearMonth.from(pagamento.getDataPagamento()).equals(mes)) continue;
            resumo.merge(titulo.getCategoria().getNome(), pagamento.getValor(), BigDecimal::add);
        }
        return resumo.entrySet().stream().sorted(Map.Entry.<String,BigDecimal>comparingByValue().reversed())
            .map(e -> new CategoriaResumo(e.getKey(),e.getValue())).toList();
    }
    private void registrar(String acao, String detalhes) {
        AuditLog log = new AuditLog(); log.setEmpresaId(acesso.id());
        log.setUsuarioId(acesso.principal().id()); log.setAcao(acao);log.setDetalhes(detalhes);auditoria.save(log);
    }
}
