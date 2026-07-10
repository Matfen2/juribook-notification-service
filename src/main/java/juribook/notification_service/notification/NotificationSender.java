package juribook.notification_service.notification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface NotificationSender {

    void sendSlotReleasedNotification(Long clientId, Long lawyerId, Long slotId);

    void sendBookingConfirmedEmail(
        String toEmail, String clientName, String lawyerName,
        LocalDate date, LocalTime startTime, LocalTime endTime
    );

    void sendNewBookingRequestEmail(
        String toEmail, String lawyerName, String clientName,
        LocalDate date, LocalTime startTime, LocalTime endTime, String reason
    );

    void sendReminderEmail(
        String toEmail, String clientName, String lawyerName,
        LocalDate date, LocalTime startTime, LocalTime endTime
    );

    void sendCancellationEmail(
        String toEmail, String clientName, String lawyerName,
        LocalDate date, LocalTime startTime, LocalTime endTime
    );

    void sendDocumentReadyEmail(
        String toEmail,
        String clientName,
        String lawyerName,
        String filename
    );

    /** Alerte admin sur détection d'abus. */
    void sendAbuseAlertEmail(
        String adminEmail,
        Long actorId,
        String reason,
        long signalCount,
        LocalDateTime occurredAt
    );
}