package br.com.espiritolabs.espirix.security;
import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:securitytests;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.task.scheduling.enabled=false"})
class ApiSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CompanyRepository companies;
    @Test void dadosFinanceirosNaoSaoPublicos() throws Exception {
        mvc.perform(get("/api/dashboard/resumo")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/importacoes")).andExpect(status().isForbidden());
    }
    @Test void financeiroNaoPodeApagarAmbiente() throws Exception {
        var user=new UserAccount();user.setId(100L);user.setNome("Teste");
        user.setEmail("financeiro@exemplo.invalid");user.setSenhaHash("x");user.setPerfil(UserRole.FINANCEIRO);
        user.setEmpresa(companies.findBySlug("trabalho").orElseThrow());
        mvc.perform(get("/api/admin/limpeza/preview").with(user(new EspirixPrincipal(user))))
            .andExpect(status().isForbidden());
    }
}
