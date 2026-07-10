package juribook.notification_service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.notification_service.service.AdminAbuseAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AbuseEventConsumer {

    private final ObjectMapper objectMapper;
    private final AdminAbuseAlertService adminAbuseAlertService;

    @KafkaListener(topics = "abuse-events", groupId = "${spring.kafka.consumer.group-id}")
    public void onAbuseEvent(String payload) {
        AbuseEvent event;
        try {
            event = objectMapper.readValue(payload, AbuseEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Impossible de désérialiser un message du topic abuse-events : {}", payload, e);
            return;
        }

        if (!"abuse.detected".equals(event.eventType())) {
            return;
        }

        adminAbuseAlertService.alertAdmin(event);
    }
}