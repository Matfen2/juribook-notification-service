package juribook.notification_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Swagger UI du notification-service.
 *
 * Pas de schéma de sécurité JWT déclaré ici (contrairement aux autres
 * services) : ce service ne valide aucun token pour l'instant, cf.
 * SecurityConfig.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("JuriBook - Notification Service")
                .description("Consomme les événements Kafka (booking-events, slot-events) et notifie les utilisateurs concernés")
                .version("0.0.1-SNAPSHOT")
                .contact(new Contact()
                    .name("Mathieu FENOUIL")
                    .email("matfen3.05@gmail.com")));
    }
}