package juribook.notification_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme le topic booking-events publié par le booking-service
 * (booking.created, booking.confirmed, booking.cancelled).
 *
 * Lecture, routage par type d'événement, et log, pas
 * encore d'envoi de notification réelle. Chaque handler de routage
 * (handleBookingCreated/Confirmed/Cancelled) est le point d'extension
 * prévu pour les sprints suivants :
 *   - email de confirmation au client (booking.confirmed)
 *   - email à l'avocat pour une nouvelle demande (booking.created)
 *
 * Même pattern que SlotReleasedEventConsumer : un topic,
 * un consumer dédié, désérialisation défensive (un message illisible
 * est loggué et ignoré plutôt que de faire planter le listener).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final ObjectMapper objectMapper;

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
        log.info("[ROUTAGE] booking.created → à notifier : l'avocat (nouvelle demande en attente), bookingId={}, lawyerId={}",
                event.bookingId(), event.lawyerId());
        // Envoi d'email réel à l'avocat : Sprint 5.4
    }

    private void handleBookingConfirmed(BookingEvent event) {
        log.info("[ROUTAGE] booking.confirmed → à notifier : le client (réservation confirmée), bookingId={}, clientId={}",
                event.bookingId(), event.clientId());
        // Envoi d'email réel au client : Sprint 5.3
    }

    private void handleBookingCancelled(BookingEvent event) {
        log.info("[ROUTAGE] booking.cancelled → à notifier : client et avocat (annulation), bookingId={}, clientId={}, lawyerId={}",
                event.bookingId(), event.clientId(), event.lawyerId());
        // Envoi d'email réel : sprint à venir
    }
}