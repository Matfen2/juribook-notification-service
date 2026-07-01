package juribook.notification_service.event;

import java.time.LocalDateTime;

/**
 * Miroir côté consumer du payload publié par le booking-service sur
 * slot-events (cf. juribook.booking_service.event.SlotReleasedEvent).
 * Les deux services ne partagent pas de module commun (database/code
 * per service), donc cette classe est dupliquée volontairement plutôt
 * que mutualisée.
 */
public record SlotReleasedEvent(
    String eventType,
    Long lawyerId,
    Long slotId,
    LocalDateTime occurredAt
) {
}