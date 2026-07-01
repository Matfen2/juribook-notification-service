package juribook.notification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration Spring Security du notification-service.
 *
 * ⚠️ Volontairement permissive pour l'instant (permitAll partout) : ce
 * service n'expose aujourd'hui aucun endpoint métier protégé — sa seule
 * fonction (Sprint 4.9) est de consommer Kafka (slot-events) et
 * d'appeler le booking-service en sortant (RestClient), rien à
 * authentifier côté entrant. Spring Security est quand même sur le
 * classpath pour éviter la génération d'un mot de passe aléatoire par
 * défaut (comportement Spring Boot si spring-boot-starter-security est
 * présent sans SecurityFilterChain explicite) et pour préparer le
 * terrain d'un futur sprint (notifications in-app consultables via API
 * → il faudra alors copier le pattern JwtAuthenticationFilter +
 * JwtService du booking-service/lawyer-service, et restreindre les
 * routes comme dans leurs SecurityConfig respectifs).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
                )
                .build();
    }
}