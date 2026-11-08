package br.com.espiritolabs.espirix.security;
import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
@Component @RequiredArgsConstructor
public class Bootstrap implements ApplicationRunner {
    private final CompanyRepository companies;private final UserAccountRepository users;private final PasswordEncoder encoder;
    @Value("${app.seed.master-email:}") private String email;
    @Value("${app.seed.master-password:}") private String password;
    @Override @Transactional public void run(ApplicationArguments args){
        companies.findBySlug("demo").orElseGet(()->criar("demo","Demonstração"));
        companies.findBySlug("trabalho").orElseGet(()->criar("trabalho","Ambiente de trabalho"));
        if(email.isBlank() || password.isBlank()) return; // Configure via variaveis de ambiente.
        if(password.length()<12) throw new IllegalStateException("ESPIRIX_MASTER_PASSWORD deve ter pelo menos 12 caracteres");
        String e=email.toLowerCase(Locale.ROOT).trim();
        if(users.findByEmailIgnoreCase(e).isEmpty()) {
            UserAccount u=new UserAccount();u.setEmail(e);u.setNome("Administrador Master");
            u.setSenhaHash(encoder.encode(password));u.setPerfil(UserRole.ADMIN_MASTER);u.setAtivo(true);users.save(u);
        }
    }
    private Company criar(String slug,String nome){Company c=new Company();c.setSlug(slug);c.setNome(nome);return companies.save(c);}
}
