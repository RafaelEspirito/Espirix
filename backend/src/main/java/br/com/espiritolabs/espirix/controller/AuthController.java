package br.com.espiritolabs.espirix.controller;
import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.repository.*;
import br.com.espiritolabs.espirix.security.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final EmpresaAccess empresa;
    private final CompanyRepository companies;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    public record EmpresaDto(Long id,String slug,String nome) { }
    public record Me(Long id,String nome,String email,UserRole perfil, EmpresaDto empresa) { }
    public record TrocarEmpresa(@NotNull Long empresaId) { }
    public record NovoUsuario(@NotBlank @Email String email,@NotBlank String nome,
        @NotBlank String senha,@NotNull UserRole perfil) { }
    public record UsuarioDto(Long id,String email,String nome,UserRole perfil,boolean ativo) { }
    public record AlterarSenha(@NotBlank String senhaAtual, @NotBlank String novaSenha) { }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken());}
    @GetMapping("/me") public Me me(){
        EspirixPrincipal p=empresa.principal();
        Long id=empresa.id();
        Company c=companies.findById(id).orElseThrow();
        return new Me(p.id(),p.nome(),p.getUsername(),p.perfil(),new EmpresaDto(c.getId(),c.getSlug(),c.getNome()));
    }
    @GetMapping("/empresas") public List<EmpresaDto> empresas(){
        if(empresa.principal().perfil()!=UserRole.ADMIN_MASTER) throw new AccessDeniedException("Acesso negado");
        return companies.findAll().stream().map(c->new EmpresaDto(c.getId(),c.getSlug(),c.getNome())).toList();
    }
    @PostMapping("/workspace") public Me trocar(@Valid @RequestBody TrocarEmpresa body,HttpServletRequest request){
        if(empresa.principal().perfil()!=UserRole.ADMIN_MASTER) throw new AccessDeniedException("Acesso negado");
        Company c=companies.findById(body.empresaId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        request.getSession().setAttribute(EmpresaAccess.EMPRESA_ATIVA,c.getId());
        return me();
    }
    @PostMapping("/senha") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
    public void alterarMinhaSenha(@Valid @RequestBody AlterarSenha body) {
        Long usuarioId = empresa.principal().id();
        UserAccount usuario = users.findById(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(body.senhaAtual(), usuario.getSenhaHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
        if (body.novaSenha().length() < 12 || body.novaSenha().length() > 128)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nova senha precisa ter 12 a 128 caracteres");
        if (passwordEncoder.matches(body.novaSenha(), usuario.getSenhaHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma senha diferente da atual");
        usuario.setSenhaHash(passwordEncoder.encode(body.novaSenha()));
        users.save(usuario);
    }
    @GetMapping("/usuarios") public List<UsuarioDto> usuarios(){
        if(!empresa.admin()) throw new AccessDeniedException("Acesso negado");
        return users.findByEmpresaIdOrderByNomeAsc(empresa.id()).stream()
          .map(u->new UsuarioDto(u.getId(),u.getEmail(),u.getNome(),u.getPerfil(),u.isAtivo())).toList();
    }
    @PostMapping("/usuarios") @ResponseStatus(HttpStatus.CREATED) @Transactional
    public UsuarioDto cadastrar(@Valid @RequestBody NovoUsuario body){
        if(!empresa.admin()) throw new AccessDeniedException("Acesso negado");
        if(body.senha().length()<12 || body.senha().length()>128) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A senha precisa ter 12 a 128 caracteres");
        if(body.perfil()==UserRole.ADMIN_MASTER) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Nao permitido criar AdminMaster");
        if(empresa.principal().perfil()==UserRole.ADMIN && body.perfil()==UserRole.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Apenas o master cria administradores");
        String email=body.email().trim().toLowerCase(Locale.ROOT);
        if(users.findByEmailIgnoreCase(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Nao foi possivel cadastrar este usuario");
        Company company=companies.findById(empresa.id()).orElseThrow();
        UserAccount u=new UserAccount();u.setEmail(email);u.setNome(body.nome().trim());u.setSenhaHash(passwordEncoder.encode(body.senha()));u.setPerfil(body.perfil());u.setEmpresa(company);u.setAtivo(true);
        users.save(u);
        return new UsuarioDto(u.getId(),u.getEmail(),u.getNome(),u.getPerfil(),u.isAtivo());
    }
}
