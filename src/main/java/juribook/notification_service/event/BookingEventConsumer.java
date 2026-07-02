package juribook.notification_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.notification_service.service.BookingConfirmationNotificationService;
import juribook.notification_service.service.BookingRequestNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme le topic booking-events publié par le booking-service
 * (booking.created, booking.confirmed, booking.cancelled).
 *
 * Sprint 5.2 : lecture, routage, log, pour tous les types d'événement.
 * Sprint 5.3 : booking.confirmed → email de confirmation au client.
 * Sprint 5.4 : booking.created → email de nouvelle demande à l'avocat.
 *
 * Le second cas de booking.cancelled (avocat/client notifiés de
 * l'annulation) reste au stade "routage + log", pas dans le scope de
 * ces sprints.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final ObjectMapper objectMapper;
    private final BookingConfirmationNotificationService confirmationNotificationService;
    private final BookingRequestNotificationService requestNotificationService;

    @KafkaListener(topics = "booking-events", groupId = "${spring.kafka.consumer.group-id}")
    public void onBookingEvent(String payload) {
        BookingEvent event;
        try {
            event = objectMapper.readValue(payload, BookingEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Impossible de désérialiser un message du topic booking-events : {}", payload, e);
            return;
        }

        log.info("Événement booking-events reçu : type={}, bookingId={}, clientId={}, lawyerId={}, timeSlotId={}, status={}",
                event.eventType(), event.bookingId(), event.clientId(), event.lawyerId(),
                event.timeSlotId(), event.status());

        switch (event.eventType()) {
            case "booking.created"   -> handleBookingCreated(event);
            case "booking.confirmed" -> handleBookingConfirmed(event);
            case "booking.cancelled" -> handleBookingCancelled(event);
            default -> log.debug("Événement booking-events ignoré (eventType inconnu={})", event.eventType());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  Routage par type d'événement
    // ══════════════════════════════════════════════════════════
    private void handleBookingCreated(BookingEvent event) {
        log.info("[ROUTAGE] booking.created -> à notifier : l'avocat (nouvelle demande en attente), bookingId={}, lawyerId={}",
                event.bookingId(), event.lawyerId());
        requestNotificationService.notifyLawyerOfNewRequest(event);
    }

    private void handleBookingConfirmed(BookingEvent event) {
        log.info("[ROUTAGE] booking.confirmed -> à notifier : le client (réservation confirmée), bookingId={}, clientId={}",
                event.bookingId(), event.clientId());
        confirmationNotificationService.notifyClientOfConfirmation(event);
    }

    private void handleBookingCancelled(BookingEvent event) {
        log.info("[ROUTAGE] booking.cancelled -> à notifier : client et avocat (annulation), bookingId={}, clientId={}, lawyerId={}",
                event.bookingId(), event.clientId(), event.lawyerId());
        // Envoi d'email réel : sprint à venir
    }
}