package br.com.espiritolabs.espirix.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cloud")
public class CloudStartupGuard {
    @Bean
    ApplicationRunner requireCloudCredentials(
            @Value("${ESPIRIX_MASTER_EMAIL:}") String masterEmail,
            @Value("${ESPIRIX_MASTER_PASSWORD:}") String masterPassword,
            @Value("${DB_PASSWORD:}") String dbPassword,
            @Value("${CORS_ORIGINS:}") String allowedOrigin) {
        return args -> {
            if (masterEmail.isBlank() || masterPassword.length() < 16 || dbPassword.length() < 20
                    || !allowedOrigin.startsWith("https://") || allowedOrigin.contains("*")) {
                throw new IllegalStateException("Cloud: credenciais e origem HTTPS fortes obrigatorias. Confira .env privado.");
            }
        };
    }
}
