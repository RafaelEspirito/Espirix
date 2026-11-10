package br.com.espiritolabs.espirix.service;

import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.dto.ApiDtos.ClienteResponse;
import br.com.espiritolabs.espirix.exception.BusinessException;
import br.com.espiritolabs.espirix.repository.*;
import br.com.espiritolabs.espirix.security.EmpresaAccess;
import br.com.espiritolabs.espirix.util.FinanceUtils;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ClientRegistrationService {
    private final ClientRepository clientes;
    private final AuditLogRepository auditoria;
    private final EmpresaAccess acesso;
    public record CadastroRequest(@NotBlank @Size(max=200) String razaoSocial,
            @Size(max=200) String nomeFantasia, @Size(max=18) String cnpjCpf,
            @Email @Size(max=200) String emailCobranca,
            @Size(max=150) String responsavelVenda, Boolean cliente, Boolean fornecedor) {}

    private String limpa(String v) { return v == null || v.isBlank() ? null : v.strip(); }
    private String chave(String nome, String documento) { return FinanceUtils.clienteKey(documento,nome); }
    private String doc(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String result = valor.replaceAll("\\D", "");
        if (result.length() != 11 && result.length() != 14) throw BusinessException.badRequest("CPF/CNPJ deve ter 11 ou 14 digitos");
        return result;
    }
    @Transactional
    public ClienteResponse criar(CadastroRequest request) {
        String nome = request.razaoSocial().strip();
        String chave = chave(nome,doc(request.cnpjCpf()));
        if (clientes.findByEmpresaIdAndChaveBusca(acesso.id(),chave).isPresent())
            throw BusinessException.badRequest("Cliente/fornecedor ja cadastrado");
        Client client = new Client(); client.setEmpresaId(acesso.id());
        aplicar(client,request);client.setChaveBusca(chave);
        clientes.saveAndFlush(client);
        audit("CADASTRO_CLIENTE",client.getId());
        return DtoMapper.cliente(client);
    }
    @Transactional
    public ClienteResponse editar(Long id, CadastroRequest request) {
        Client client = clientes.findByIdAndEmpresaId(id,acesso.id())
            .orElseThrow(() -> BusinessException.notFound("Cliente nao encontrado"));
        // Preserve a chave de identidade ao renomear uma contraparte importada.
        // Assim, uma nova importacao OMIE nao recria um cliente apenas porque seu nome mudou.
        String novoDocumento = doc(request.cnpjCpf());
        String novaChave = java.util.Objects.equals(client.getCnpjCpf(), novoDocumento)
            ? client.getChaveBusca() : chave(request.razaoSocial(), novoDocumento);
        clientes.findByEmpresaIdAndChaveBusca(acesso.id(),novaChave)
            .filter(outro -> !outro.getId().equals(id))
            .ifPresent(outro -> {throw BusinessException.badRequest("Razao social ja cadastrada");});
        aplicar(client,request); client.setChaveBusca(novaChave);
        clientes.saveAndFlush(client);
        audit("EDITAR_CLIENTE",id);
        return DtoMapper.cliente(client);
    }
    private void aplicar(Client c,CadastroRequest r) {
        c.setRazaoSocial(r.razaoSocial().strip()); c.setNomeFantasia(limpa(r.nomeFantasia()));
        c.setCnpjCpf(doc(r.cnpjCpf()));
        c.setEmailCobranca(limpa(r.emailCobranca()));
        c.setResponsavelVenda(limpa(r.responsavelVenda()));
        // Clientes antigos da API continuam validos; cadastro novo define papeis explicitamente.
        if (r.cliente() != null) c.setCliente(r.cliente());
        if (r.fornecedor() != null) c.setFornecedor(r.fornecedor());
        if (!c.isCliente() && !c.isFornecedor())
            throw BusinessException.badRequest("Selecione cliente e/ou fornecedor");
    }
    private void audit(String action, Long id) {
        AuditLog log = new AuditLog();log.setEmpresaId(acesso.id());
        log.setUsuarioId(acesso.principal().id());log.setAcao(action);log.setDetalhes("cliente="+id);
        auditoria.save(log);
    }
}
