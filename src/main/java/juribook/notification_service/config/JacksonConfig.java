package juribook.notification_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fournit explicitement un bean ObjectMapper (Jackson 2).
 *
 * Nécessaire car ce service n'a aucun @RestController : l'autoconfiguration
 * Spring Boot 4 qui construit normalement l'ObjectMapper classique est
 * liée au support MVC complet (Jackson2ObjectMapperBuilder, déclenché par
 * la présence de endpoints REST). Sans ça, seul un JsonMapper (Jackson 3)
 * est autoconfiguré par défaut, et SlotReleasedEventConsumer (qui attend
 * un ObjectMapper classique pour désérialiser les événements Kafka) ne
 * trouve pas de bean à injecter.
 *
 * findAndRegisterModules() découvre et enregistre automatiquement les
 * modules Jackson disponibles sur le classpath (ex: JavaTimeModule pour
 * LocalDateTime, si jackson-datatype-jsr310 y est) sans dépendance dure
 * explicite dans le code.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}