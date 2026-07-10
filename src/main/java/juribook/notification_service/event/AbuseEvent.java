package juribook.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AbuseEvent(
    String eventType,
    Long actorId,
    String reason,
    long signalCount,
    LocalDateTime occurredAt
) {
}