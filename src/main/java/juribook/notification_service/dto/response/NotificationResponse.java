package juribook.notification_service.dto.response;

import juribook.notification_service.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    String type,
    String message,
    Long bookingId,
    boolean read,
    LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType().name(), n.getMessage(),
                n.getBookingId(), n.isRead(), n.getCreatedAt()
        );
    }
}