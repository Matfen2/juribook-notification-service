package juribook.notification_service.notification;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Abstraction de l'envoi de notification à un utilisateur.
 *
 * Une seule implémentation : EmailNotificationSender, qui envoie
 * réellement des emails via JavaMailSender depuis le Sprint 5.3 (avant
 * ça, stub qui logguait uniquement). La notification in-app (persistée,
 * consultable via API par le frontend) reste repoussée à un sprint dédié.
 */
public interface NotificationSender {

    void sendSlotReleasedNotification(Long clientId, Long lawyerId, Long slotId);

    /**
     * Email de confirmation envoyé au client quand l'avocat confirme sa
     * demande de réservation.
     */
    void sendBookingConfirmedEmail(
        String toEmail,
        String clientName,
        String lawyerName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    );
}