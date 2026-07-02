package juribook.notification_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@Slf4j
public class RestClientLawyerServiceClient implements LawyerServiceClient {

    private final RestClient restClient;

    public RestClientLawyerServiceClient(@Value("${lawyer-service.base-url}") String lawyerServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(lawyerServiceBaseUrl)
                .build();
    }

    @Override
    public Optional<LawyerProfileDto> getLawyer(Long lawyerId) {
        try {
            LawyerProfileDto lawyer = restClient.get()
                    .uri("/api/lawyers/{lawyerId}", lawyerId)
                    .retrieve()
                    .body(LawyerProfileDto.class);

            return Optional.ofNullable(lawyer);
        } catch (RestClientException e) {
            log.error("Échec de l'appel au lawyer-service pour résoudre l'avocat (lawyerId={})",
                    lawyerId, e);
            return Optional.empty();
        }
    }
}