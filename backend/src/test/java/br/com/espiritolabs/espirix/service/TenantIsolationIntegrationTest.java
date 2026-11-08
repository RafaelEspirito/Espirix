package br.com.espiritolabs.espirix.service;
import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.repository.*;
import br.com.espiritolabs.espirix.security.EspirixPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @Transactional
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:tenanttests;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.task.scheduling.enabled=false", "app.limpeza.permitir-trabalho=true"})
class TenantIsolationIntegrationTest {
    @Autowired CompanyRepository companies;
    @Autowired ImportService imports;
    @Autowired InvoiceRepository invoices;
    @Autowired DashboardService dashboard;
    @Autowired InvoiceService titles;
    @Autowired WorkspaceClearService limpeza;
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void como(String slug, UserRole role){
        var u=new UserAccount();u.setId(100L);u.setNome("Teste");u.setEmail("teste@invalid.local");
        u.setSenhaHash("x");u.setPerfil(role);u.setEmpresa(companies.findBySlug(slug).orElseThrow());
        var p=new EspirixPrincipal(u);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,p.getAuthorities()));
    }
    byte[] csv(){
        var fmt=DateTimeFormatter.ofPattern("dd/MM/yyyy");
        var hoje=LocalDate.now();
        String a="Razão Social;Número Documento;Data Emissão;Data Vencimento;Valor Original\n"+
          "Cliente Igual;NF-1;"+hoje.minusDays(2).format(fmt)+";"+hoje.plusDays(5).format(fmt)+";800,00\n";
        return a.getBytes(StandardCharsets.UTF_8);
    }
    @Test void isolamentoEExclusaoNaoAtingemOutraEmpresa() throws Exception {
        como("demo", UserRole.FINANCEIRO);
        imports.importar("d.csv",OrigemArquivo.CSV,csv(),"a",null);
        Long demoId=invoices.findAll().stream().filter(t->t.getEmpresaId().equals(companies.findBySlug("demo").orElseThrow().getId())).findFirst().orElseThrow().getId();
        como("trabalho", UserRole.FINANCEIRO);
        assertThat(dashboard.resumo(TipoOperacao.RECEBER).dividaAtiva()).isEqualByComparingTo("0.00");
        assertThatThrownBy(()->titles.buscar(demoId)).isInstanceOf(RuntimeException.class);
        imports.importar("t.csv",OrigemArquivo.CSV,csv(),"b",null);
        assertThat(dashboard.resumo(TipoOperacao.RECEBER).dividaAtiva()).isEqualByComparingTo("800.00");
        como("demo", UserRole.ADMIN);
        assertThat(limpeza.preview().titulos()).isEqualTo(1L);
        limpeza.limpar(new WorkspaceClearService.Pedido("APAGAR DEMO"));
        assertThat(dashboard.resumo(TipoOperacao.RECEBER).dividaAtiva()).isEqualByComparingTo("0.00");
        como("trabalho", UserRole.FINANCEIRO);
        assertThat(dashboard.resumo(TipoOperacao.RECEBER).dividaAtiva()).isEqualByComparingTo("800.00");
    }
}
