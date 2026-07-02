package juribook.notification_service.notification;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Abstraction de l'envoi de notification à un utilisateur.
 * Une seule implémentation : EmailNotificationSender.
 */
public interface NotificationSender {

    void sendSlotReleasedNotification(Long clientId, Long lawyerId, Long slotId);

    /** Email de confirmation au client quand l'avocat confirme. */
    void sendBookingConfirmedEmail(
        String toEmail,
        String clientName,
        String lawyerName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    );

    /** Email à l'avocat pour une nouvelle demande PENDING. */
    void sendNewBookingRequestEmail(
        String toEmail,
        String lawyerName,
        String clientName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String reason
    );

    /** Rappel automatique au client, 24h avant le rendez-vous. */
    void sendReminderEmail(
        String toEmail,
        String clientName,
        String lawyerName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    );
}