package br.com.espiritolabs.espirix.security;
import br.com.espiritolabs.espirix.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
@Configuration @EnableMethodSecurity @RequiredArgsConstructor
public class SecurityConfig {
    private final UserAccountRepository users;
    private final CompanyRepository companies;
    @Bean public PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
    @Bean public UserDetailsService userDetailsService(){return email-> users.findByEmailIgnoreCase(email)
        .map(EspirixPrincipal::new).orElseThrow(()->new UsernameNotFoundException("Credenciais invalidas"));}
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.httpBasic(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(a->a
            .requestMatchers("/api/auth/csrf","/api/auth/login","/actuator/health","/error").permitAll()
            .requestMatchers("/api/auth/workspace","/api/auth/empresas").hasRole("ADMIN_MASTER")
            .requestMatchers("/api/auth/usuarios","/api/auth/usuarios/**","/api/admin/**").hasAnyRole("ADMIN_MASTER","ADMIN")
            .requestMatchers(HttpMethod.POST,"/api/importacoes/**","/api/titulos/**").hasAnyRole("ADMIN_MASTER","ADMIN","FINANCEIRO")
            .requestMatchers("/api/**").authenticated()
            .requestMatchers("/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").hasRole("ADMIN_MASTER")
            .anyRequest().denyAll())
        .exceptionHandling(e->e
            .authenticationEntryPoint((req,res,ex)->res.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Autenticacao obrigatoria"))
            .accessDeniedHandler((req,res,ex)->res.sendError(HttpServletResponse.SC_FORBIDDEN,"Acesso negado")))
        .formLogin(f->f.loginProcessingUrl("/api/auth/login")
            .successHandler((req,res,auth)->{
                EspirixPrincipal p=(EspirixPrincipal)auth.getPrincipal();
                if(p.perfil()==br.com.espiritolabs.espirix.domain.UserRole.ADMIN_MASTER){
                    companies.findBySlug("demo").ifPresent(c->req.getSession().setAttribute(EmpresaAccess.EMPRESA_ATIVA,c.getId()));
                }
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"autenticado\":true}");
            })
            .failureHandler((req,res,ex)->res.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Credenciais invalidas")))
        .logout(l->l.logoutUrl("/api/auth/logout")
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
        .sessionManagement(s->s.sessionFixation().migrateSession());
        // Protecao CSRF padrao ATIVA para sessao/cookie, inclusive login e exclusao.
        return http.build();
    }
}
