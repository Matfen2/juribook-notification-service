package juribook.notification_service.service;

import juribook.notification_service.client.AuthServiceClient;
import juribook.notification_service.client.BookingDetailsDto;
import juribook.notification_service.client.BookingServiceClient;
import juribook.notification_service.client.LawyerProfileDto;
import juribook.notification_service.client.LawyerServiceClient;
import juribook.notification_service.client.UserContactDto;
import juribook.notification_service.event.BookingEvent;
import juribook.notification_service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestration de l'email de confirmation client.
 *
 * L'événement booking.confirmed ne transporte que des ids (bookingId,
 * clientId, lawyerId, timeSlotId), trois appels inter-services sont
 * nécessaires pour composer un email lisible :
 *   1. booking-service : date/heure du créneau (BookingServiceClient)
 *   2. lawyer-service   : nom de l'avocat (LawyerServiceClient)
 *   3. auth-service      : nom + email du client (AuthServiceClient)
 *
 * Si l'un des trois échoue, l'email n'est pas envoyé mais le traitement
 * de l'événement Kafka ne plante pas pour autant (chaque client HTTP est
 * déjà défensif individuellement, cf. leurs implémentations).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingConfirmationNotificationService {

    private final BookingServiceClient bookingServiceClient;
    private final LawyerServiceClient lawyerServiceClient;
    private final AuthServiceClient authServiceClient;
    private final NotificationSender notificationSender;

    public void notifyClientOfConfirmation(BookingEvent event) {
        Optional<BookingDetailsDto> details = bookingServiceClient.getBookingDetails(event.bookingId());
        if (details.isEmpty() || details.get().date() == null || details.get().startTime() == null) {
            log.warn("Détail de créneau introuvable pour bookingId={}, email de confirmation non envoyé",
                    event.bookingId());
            return;
        }

        Optional<LawyerProfileDto> lawyer = lawyerServiceClient.getLawyer(event.lawyerId());
        if (lawyer.isEmpty()) {
            log.warn("Avocat introuvable (lawyerId={}) pour bookingId={}, email de confirmation non envoyé",
                    event.lawyerId(), event.bookingId());
            return;
        }

        Optional<UserContactDto> client = authServiceClient.getContact(event.clientId());
        if (client.isEmpty()) {
            log.warn("Client introuvable (clientId={}) pour bookingId={}, email de confirmation non envoyé",
                    event.clientId(), event.bookingId());
            return;
        }

        BookingDetailsDto d = details.get();
        notificationSender.sendBookingConfirmedEmail(
                client.get().email(),
                client.get().name(),
                lawyer.get().name(),
                d.date(),
                d.startTime(),
                d.endTime()
        );
    }
}