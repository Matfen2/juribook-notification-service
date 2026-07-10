package juribook.notification_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.notification_service.service.AdminAbuseAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AbuseEventConsumerTest {

    @Mock
    private AdminAbuseAlertService adminAbuseAlertService;

    private AbuseEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AbuseEventConsumer(new ObjectMapper().findAndRegisterModules(), adminAbuseAlertService);
    }

    @Test
    @DisplayName("abuse.detected - alerte l'admin")
    void onAbuseEvent_detected_alertsAdmin() {
        String payload = """
            {"eventType":"abuse.detected","actorId":4,"reason":"Plus de 5 annulations en 7 jours",
             "signalCount":6,"occurredAt":"2026-07-05T10:00:00"}
            """;

        consumer.onAbuseEvent(payload);

        verify(adminAbuseAlertService).alertAdmin(any(AbuseEvent.class));
    }

    @Test
    @DisplayName("JSON malformé - ne plante jamais")
    void onAbuseEvent_malformedJson_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onAbuseEvent("pas du JSON"));
        verify(adminAbuseAlertService, never()).alertAdmin(any());
    }
}