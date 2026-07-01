package juribook.notification_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.notification_service.service.SlotReleaseNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme le topic slot-events publié par le booking-service.
 *
 * Pour l'instant, un seul type d'événement y transite : slot.released
 * (Sprint 4.7 côté booking-service). Le filtre sur eventType est là par
 * précaution / extensibilité plutôt que par nécessité actuelle.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlotReleasedEventConsumer {

    private final ObjectMapper objectMapper;
    private final SlotReleaseNotificationService notificationService;

    @KafkaListener(topics = "slot-events", groupId = "${spring.kafka.consumer.group-id}")
    public void onSlotEvent(String payload) {
        SlotReleasedEvent event;
        try {
            event = objectMapper.readValue(payload, SlotReleasedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Impossible de désérialiser un message du topic slot-events : {}", payload, e);
            return;
        }

        if (!"slot.released".equals(event.eventType())) {
            log.debug("Événement slot-events ignoré (eventType={})", event.eventType());
            return;
        }

        log.info("Événement slot.released reçu : lawyerId={}, slotId={}",
                event.lawyerId(), event.slotId());
        notificationService.notifyWaitlistedClients(event.lawyerId(), event.slotId());
    }
}