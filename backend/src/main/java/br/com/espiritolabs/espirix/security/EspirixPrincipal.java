package br.com.espiritolabs.espirix.security;
import br.com.espiritolabs.espirix.domain.*;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.*;
public class EspirixPrincipal implements UserDetails {
    private final Long id, empresaId;
    private final String email, senhaHash, nome;
    private final UserRole perfil;
    private final boolean ativo;
    public EspirixPrincipal(UserAccount u) {
        id=u.getId(); empresaId=u.getEmpresa()==null?null:u.getEmpresa().getId();
        email=u.getEmail(); senhaHash=u.getSenhaHash(); nome=u.getNome();
        perfil=u.getPerfil(); ativo=u.isAtivo();
    }
    public Long id(){return id;} public Long empresaId(){return empresaId;}
    public String nome(){return nome;} public UserRole perfil(){return perfil;}
    @Override public String getUsername(){return email;}
    @Override public String getPassword(){return senhaHash;}
    @Override public boolean isEnabled(){return ativo;}
    @Override public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of((GrantedAuthority) () -> "ROLE_"+perfil.name());
    }
}
