package juribook.notification_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.notification_service.service.DocumentReadyNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme document-events (Sprint 6.7) — seul document.ready déclenche
 * une notification client, document.uploaded est ignoré (le client
 * sait déjà qu'il vient d'envoyer le fichier, pas besoin de le lui
 * confirmer une seconde fois côté notification).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventConsumer {

    private final ObjectMapper objectMapper;
    private final DocumentReadyNotificationService documentReadyNotificationService;

    @KafkaListener(topics = "document-events", groupId = "${spring.kafka.consumer.group-id}")
    public void onDocumentEvent(String payload) {
        DocumentEvent event;
        try {
            event = objectMapper.readValue(payload, DocumentEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Impossible de désérialiser un message du topic document-events : {}", payload, e);
            return;
        }

        log.info("Événement document-events reçu : type={}, documentId={}, bookingId={}",
                event.eventType(), event.documentId(), event.bookingId());

        if ("document.ready".equals(event.eventType())) {
            documentReadyNotificationService.notifyClientDocumentReady(event);
        } else {
            log.debug("Événement document-events ignoré (eventType={})", event.eventType());
        }
    }
}