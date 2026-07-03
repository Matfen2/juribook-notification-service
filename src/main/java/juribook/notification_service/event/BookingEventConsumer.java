package juribook.notification_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.notification_service.service.BookingCancellationNotificationService;
import juribook.notification_service.service.BookingConfirmationNotificationService;
import juribook.notification_service.service.BookingReminderNotificationService;
import juribook.notification_service.service.BookingRequestNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme le topic booking-events publié par le booking-service.
 *
 * booking.confirmed → email de confirmation au client.
 * booking.created → email de nouvelle demande à l'avocat.
 * booking.reminder → email de rappel 24h au client.
 * booking.cancelled → email d'annulation au client (couvre refus, annulation manuelle, et désactivation d'avocat).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final ObjectMapper objectMapper;
    private final BookingConfirmationNotificationService confirmationNotificationService;
    private final BookingRequestNotificationService requestNotificationService;
    private final BookingReminderNotificationService reminderNotificationService;
    private final BookingCancellationNotificationService cancellationNotificationService;

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
            case "booking.reminder"  -> handleBookingReminder(event);
            default -> log.debug("Événement booking-events ignoré (eventType inconnu={})", event.eventType());
        }
    }

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
        log.info("[ROUTAGE] booking.cancelled -> à notifier : le client (annulation), bookingId={}, clientId={}",
                event.bookingId(), event.clientId());
        cancellationNotificationService.notifyClientOfCancellation(event);
    }

    private void handleBookingReminder(BookingEvent event) {
        log.info("[ROUTAGE] booking.reminder -> à notifier : le client (rappel 24h), bookingId={}, clientId={}",
                event.bookingId(), event.clientId());
        reminderNotificationService.notifyClientOfReminder(event);
    }
}