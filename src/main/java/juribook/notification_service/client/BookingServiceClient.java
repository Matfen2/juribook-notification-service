package juribook.notification_service.client;

import java.util.List;

/**
 * Abstraction de l'appel inter-services vers le booking-service, pour
 * résoudre la liste d'attente d'un avocat. Une seule implémentation
 * pour l'instant (RestClientBookingServiceClient) ; l'interface existe
 * surtout pour permettre de mocker facilement dans les tests.
 */
public interface BookingServiceClient {

    /**
     * Retourne la liste d'attente d'un avocat. Ne lève jamais
     * d'exception : en cas d'échec de l'appel HTTP (booking-service
     * injoignable, timeout...), retourne une liste vide plutôt que de
     * faire échouer le traitement de l'événement Kafka.
     */
    List<WaitlistEntryDto> getWaitlist(Long lawyerId);
}