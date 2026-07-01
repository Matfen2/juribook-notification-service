package juribook.notification_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@Slf4j
public class RestClientBookingServiceClient implements BookingServiceClient {

    private final RestClient restClient;

    public RestClientBookingServiceClient(@Value("${booking-service.base-url}") String bookingServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(bookingServiceBaseUrl)
                .build();
    }

    @Override
    public List<WaitlistEntryDto> getWaitlist(Long lawyerId) {
        try {
            WaitlistEntryDto[] entries = restClient.get()
                    .uri("/api/waitlist/{lawyerId}", lawyerId)
                    .retrieve()
                    .body(WaitlistEntryDto[].class);

            return entries != null ? List.of(entries) : List.of();
        } catch (RestClientException e) {
            log.error("Échec de l'appel au booking-service pour récupérer la liste d'attente (lawyerId={})",
                    lawyerId, e);
            return List.of();
        }
    }
}