package juribook.notification_service.client;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction de l'appel inter-services vers le booking-service.
 */
public interface BookingServiceClient {

    /**
     * Retourne la liste d'attente d'un avocat. Ne lève jamais
     * d'exception : en cas d'échec HTTP, retourne une liste vide plutôt
     * que de faire échouer le traitement de l'événement Kafka.
     */
    List<WaitlistEntryDto> getWaitlist(Long lawyerId);

    /**
     * Résout le détail enrichi (date/heure) d'une réservation par id.
     * Optional.empty() si introuvable ou en cas d'échec HTTP, le même
     * principe défensif que getWaitlist : ne jamais faire planter le
     * traitement de l'événement Kafka pour un problème d'appel externe.
     */
    Optional<BookingDetailsDto> getBookingDetails(Long bookingId);
}