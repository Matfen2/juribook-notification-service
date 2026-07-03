package juribook.notification_service.service;

import juribook.notification_service.client.AuthServiceClient;
import juribook.notification_service.client.BookingDetailsDto;
import juribook.notification_service.client.BookingServiceClient;
import juribook.notification_service.client.LawyerProfileDto;
import juribook.notification_service.client.LawyerServiceClient;
import juribook.notification_service.client.UserContactDto;
import juribook.notification_service.entity.NotificationType;
import juribook.notification_service.event.BookingEvent;
import juribook.notification_service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestration de l'annulation côté client.
 *
 * Couvre les trois origines possibles d'un booking.cancelled, refus
 * par l'avocat, annulation manuelle (client ou avocat), ou désactivation
 * de l'avocat (annulation automatique des PENDING), sans
 * distinction : le client reçoit le même email dans les trois cas,
 * l'événement Kafka ne porte de toute façon pas l'information de la
 * cause exacte.
 *
 * Contrairement aux autres orchestrateurs, tolère un détail de créneau
 * manquant (BookingDetailsDto absent ou incomplet) plutôt que
 * d'abandonner l'envoi : au moment où ce handler tourne, le TimeSlot a
 * potentiellement déjà été remis à AVAILABLE, voire réservé par
 * quelqu'un d'autre entre-temps, l'email part quand même, sans le
 * détail horaire (cf. EmailNotificationSender.sendCancellationEmail).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCancellationNotificationService {

    private final BookingServiceClient bookingServiceClient;
    private final LawyerServiceClient lawyerServiceClient;
    private final AuthServiceClient authServiceClient;
    private final NotificationSender notificationSender;
    private final NotificationService notificationService;

    public void notifyClientOfCancellation(BookingEvent event) {
        Optional<LawyerProfileDto> lawyer = lawyerServiceClient.getLawyer(event.lawyerId());
        if (lawyer.isEmpty()) {
            log.warn("Avocat introuvable (lawyerId={}) pour bookingId={}, email d'annulation non envoyé",
                    event.lawyerId(), event.bookingId());
            return;
        }

        Optional<UserContactDto> client = authServiceClient.getContact(event.clientId());
        if (client.isEmpty()) {
            log.warn("Client introuvable (clientId={}) pour bookingId={}, email d'annulation non envoyé",
                    event.clientId(), event.bookingId());
            return;
        }

        // Best-effort : le créneau peut ne plus être résolvable (déjà
        // libéré, voire déjà repris par quelqu'un d'autre), on envoie
        // quand même l'email, juste sans le détail horaire dans ce cas.
        Optional<BookingDetailsDto> details = bookingServiceClient.getBookingDetails(event.bookingId());
        BookingDetailsDto d = details.orElse(null);

        notificationSender.sendCancellationEmail(
                client.get().email(),
                client.get().name(),
                lawyer.get().name(),
                d != null ? d.date() : null,
                d != null ? d.startTime() : null,
                d != null ? d.endTime() : null
        );

        String message = "Votre rendez-vous avec %s a été annulé".formatted(lawyer.get().name());
        notificationService.createNotification(
                event.clientId(), NotificationType.BOOKING_CANCELLED, message, event.bookingId());
    }
}