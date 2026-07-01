package juribook.notification_service.client;

import java.time.LocalDateTime;

/**
 * Miroir côté notification-service de WaitlistEntryResponse
 * (booking-service). Désérialise la réponse de
 * GET /api/waitlist/{lawyerId}.
 */
public record WaitlistEntryDto(
    Long id,
    Long lawyerId,
    Long clientId,
    LocalDateTime createdAt
) {
}