package juribook.notification_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@Slf4j
public class RestClientAuthServiceClient implements AuthServiceClient {

    private final RestClient restClient;

    public RestClientAuthServiceClient(@Value("${auth-service.base-url}") String authServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(authServiceBaseUrl)
                .build();
    }

    @Override
    public Optional<UserContactDto> getContact(Long userId) {
        try {
            UserContactDto contact = restClient.get()
                    .uri("/api/users/{userId}/contact", userId)
                    .retrieve()
                    .body(UserContactDto.class);

            return Optional.ofNullable(contact);
        } catch (RestClientException e) {
            log.error("Échec de l'appel à l'auth-service pour résoudre le contact (userId={})",
                    userId, e);
            return Optional.empty();
        }
    }
}