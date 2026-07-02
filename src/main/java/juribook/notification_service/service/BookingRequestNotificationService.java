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
 * Orchestration de l'email "nouvelle demande" côté avocat.
 *
 * Un cran plus complexe que BookingConfirmationNotificationService
 * : pour joindre l'avocat par email il faut d'abord résoudre son
 * authUserId via lawyer-service (l'email n'appartient qu'à
 * l'auth-service, lawyer-service ne connaît que le nom, dénormalisé à
 * la création du profil), un saut supplémentaire.
 *
 *   1. booking-service : date/heure/motif du créneau (BookingServiceClient)
 *   2. lawyer-service   : nom + authUserId de l'avocat (LawyerServiceClient)
 *   3. auth-service      : email de l'avocat, résolu via authUserId (AuthServiceClient)
 *   4. auth-service      : nom du client, pour personnaliser le message (AuthServiceClient)
 *
 * Si l'un des appels échoue, l'email n'est pas envoyé mais le
 * traitement de l'événement Kafka ne plante pas pour autant (chaque
 * client HTTP est déjà défensif individuellement).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingRequestNotificationService {

    private final BookingServiceClient bookingServiceClient;
    private final LawyerServiceClient lawyerServiceClient;
    private final AuthServiceClient authServiceClient;
    private final NotificationSender notificationSender;

    public void notifyLawyerOfNewRequest(BookingEvent event) {
        Optional<BookingDetailsDto> details = bookingServiceClient.getBookingDetails(event.bookingId());
        if (details.isEmpty() || details.get().date() == null || details.get().startTime() == null) {
            log.warn("Détail de créneau introuvable pour bookingId={}, email de nouvelle demande non envoyé",
                    event.bookingId());
            return;
        }

        Optional<LawyerProfileDto> lawyer = lawyerServiceClient.getLawyer(event.lawyerId());
        if (lawyer.isEmpty() || lawyer.get().authUserId() == null) {
            log.warn("Avocat introuvable ou authUserId manquant (lawyerId={}) pour bookingId={}, email non envoyé",
                    event.lawyerId(), event.bookingId());
            return;
        }

        Optional<UserContactDto> lawyerContact = authServiceClient.getContact(lawyer.get().authUserId());
        if (lawyerContact.isEmpty()) {
            log.warn("Contact avocat introuvable (authUserId={}) pour bookingId={}, email non envoyé",
                    lawyer.get().authUserId(), event.bookingId());
            return;
        }

        Optional<UserContactDto> client = authServiceClient.getContact(event.clientId());
        if (client.isEmpty()) {
            log.warn("Client introuvable (clientId={}) pour bookingId={}, email non envoyé",
                    event.clientId(), event.bookingId());
            return;
        }

        BookingDetailsDto d = details.get();
        notificationSender.sendNewBookingRequestEmail(
                lawyerContact.get().email(),
                lawyer.get().name(),
                client.get().name(),
                d.date(),
                d.startTime(),
                d.endTime(),
                d.reason()
        );
    }
}