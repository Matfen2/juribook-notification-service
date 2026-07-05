package juribook.notification_service.event;

import java.time.LocalDateTime;

/**
 * Miroir de DocumentEvent (booking-service). Ce service ne s'intéresse
 * qu'à document.ready, document.uploaded est ignoré (aucune action
 * client-facing au moment de l'upload lui-même, cf. DocumentEventConsumer).
 */
public record DocumentEvent(
    String eventType,
    Long documentId,
    Long bookingId,
    Long clientId,
    Long lawyerId,
    String originalFilename,
    String contentType,
    long sizeBytes,
    String storagePath,
    LocalDateTime occurredAt
) {
}