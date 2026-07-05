package juribook.notification_service.notification;

import java.time.LocalDate;
import java.time.LocalTime;

public interface NotificationSender {

    // Email au client quand un créneau qu'il attendait est libéré par un autre client.
    void sendSlotReleasedNotification(Long clientId, Long lawyerId, Long slotId);

    // Email au client quand sa réservation a été confirmée par l'avocat.
    void sendBookingConfirmedEmail(
        String toEmail, String clientName, String lawyerName,
        LocalDate date, LocalTime startTime, LocalTime endTime
    );

    // Email à l'avocat quand un client lui envoie une demande de réservation.
    void sendNewBookingRequestEmail(
        String toEmail, String lawyerName, String clientName,
        LocalDate date, LocalTime startTime, LocalTime endTime, String reason
    );

    // Email au client quand sa réservation est annulée par l'avocat.
    void sendReminderEmail(
        String toEmail, String clientName, String lawyerName,
        LocalDate date, LocalTime startTime, LocalTime endTime
    );

    // Email au client quand sa réservation est annulée par l'avocat.
    void sendCancellationEmail(
        String toEmail, String clientName, String lawyerName,
        LocalDate date, LocalTime startTime, LocalTime endTime
    );

    /** Email au client quand son document uploadé a été traité. */
    void sendDocumentReadyEmail(
        String toEmail,
        String clientName,
        String lawyerName,
        String filename
    );
}