package br.com.espiritolabs.espirix.service;
import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.repository.*;
import br.com.espiritolabs.espirix.security.EmpresaAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service @RequiredArgsConstructor
public class WorkspaceClearService {
    private final EmpresaAccess empresa; private final CompanyRepository companies;
    private final InvoiceRepository invoices;private final PaymentRepository payments;
    private final ClientRepository clients;private final ImportBatchRepository imports;
    private final AuditLogRepository audit;
    @Value("${app.limpeza.permitir-trabalho:false}") private boolean permitirTrabalho;
    public record Preview(String empresa,String slug,long clientes,long titulos,long pagamentos,long importacoes,boolean permitido,String confirmacao) { }
    public record Pedido(String confirmacao) { }
    public Preview preview(){
        Company c=companies.findById(empresa.id()).orElseThrow();
        boolean can=c.getSlug().equals("demo") || (c.getSlug().equals("trabalho") && permitirTrabalho);
        return new Preview(c.getNome(),c.getSlug(),clients.countByEmpresaId(c.getId()),invoices.countByEmpresaId(c.getId()),
            payments.countByEmpresaId(c.getId()),imports.countByEmpresaId(c.getId()),can,"APAGAR "+c.getSlug().toUpperCase());
    }
    @Transactional
    public Preview limpar(Pedido p){
        Company c=companies.findLockedById(empresa.id()).orElseThrow();
        Preview atual=preview();
        if(!atual.permitido()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Limpeza de trabalho desabilitada no servidor");
        if(p==null || !atual.confirmacao().equals(p.confirmacao())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Frase de confirmacao incorreta");
        // Ordem de remocao preserva FKs; todas as operacoes na mesma transacao.
        int nPag=payments.apagarEmpresa(c.getId());
        int nTit=invoices.apagarEmpresa(c.getId());
        int nCli=clients.apagarEmpresa(c.getId());
        int nImp=imports.apagarEmpresa(c.getId());
        AuditLog log=new AuditLog();log.setEmpresaId(c.getId());log.setUsuarioId(empresa.principal().id());
        log.setAcao("LIMPEZA_FINANCEIRA");log.setDetalhes("pagamentos="+nPag+";titulos="+nTit+";clientes="+nCli+";importacoes="+nImp);audit.save(log);
        return preview();
    }
}
