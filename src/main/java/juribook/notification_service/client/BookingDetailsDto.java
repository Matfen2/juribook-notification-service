package juribook.notification_service.client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Miroir de BookingHistoryResponse (booking-service). Désérialise la
 * réponse de GET /api/bookings/{id}, utilisé pour résoudre la date/heure
 * d'un rendez-vous à partir d'un bookingId reçu via Kafka (l'événement ne
 * transporte que timeSlotId, pas la date/heure déjà résolue).
 */
public record BookingDetailsDto(
    Long id,
    Long lawyerId,
    Long timeSlotId,
    String status,
    String reason,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    LocalDateTime createdAt
) {
}