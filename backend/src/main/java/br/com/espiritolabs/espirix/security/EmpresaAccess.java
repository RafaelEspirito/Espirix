package br.com.espiritolabs.espirix.security;
import br.com.espiritolabs.espirix.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class EmpresaAccess {
    public static final String EMPRESA_ATIVA="espirix.empresaAtiva";
    private final HttpServletRequest request;
    public EspirixPrincipal principal(){
        Object p=SecurityContextHolder.getContext().getAuthentication()==null?null:
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!(p instanceof EspirixPrincipal e)) throw new AccessDeniedException("Autenticacao obrigatoria");
        return e;
    }
    public Long id(){
        EspirixPrincipal p=principal();
        if(p.perfil()!=UserRole.ADMIN_MASTER) return p.empresaId();
        Object id=request.getSession(false)==null?null:request.getSession(false).getAttribute(EMPRESA_ATIVA);
        if(!(id instanceof Long l)) throw new AccessDeniedException("Selecione uma empresa");
        return l;
    }
    public boolean admin(){return principal().perfil()==UserRole.ADMIN_MASTER || principal().perfil()==UserRole.ADMIN;}
}
