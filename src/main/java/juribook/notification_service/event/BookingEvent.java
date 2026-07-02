package juribook.notification_service.event;

import java.time.LocalDateTime;

/**
 * Miroir côté consumer du payload publié par le booking-service sur
 * booking-events (cf. juribook.booking_service.event.BookingEvent).
 * Même principe que SlotReleasedEvent : dupliqué volontairement plutôt
 * que mutualisé, les deux services ne partagent pas de module commun
 * (database/code per service).
 */
public record BookingEvent(
    String eventType,
    Long bookingId,
    Long clientId,
    Long lawyerId,
    Long timeSlotId,
    String status,
    String reason,
    LocalDateTime occurredAt
) {
}