package juribook.notification_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.notification_service.service.DocumentReadyNotificationService;
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
class DocumentEventConsumerTest {

    @Mock
    private DocumentReadyNotificationService documentReadyNotificationService;

    private DocumentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new DocumentEventConsumer(new ObjectMapper().findAndRegisterModules(), documentReadyNotificationService);
    }

    @Test
    @DisplayName("document.ready - déclenche la notification")
    void onDocumentEvent_ready_triggersNotification() {
        String payload = """
            {"eventType":"document.ready","documentId":1,"bookingId":7,"clientId":42,"lawyerId":4,
             "originalFilename":"contrat.pdf","contentType":"application/pdf","sizeBytes":2048,
             "storagePath":"/permanent/contrat.pdf","occurredAt":"2026-07-05T10:00:00"}
            """;

        consumer.onDocumentEvent(payload);

        verify(documentReadyNotificationService).notifyClientDocumentReady(any(DocumentEvent.class));
    }

    @Test
    @DisplayName("document.uploaded - ignoré, pas de notification à l'upload")
    void onDocumentEvent_uploaded_ignored() {
        String payload = """
            {"eventType":"document.uploaded","documentId":1,"bookingId":7,"clientId":42,"lawyerId":4,
             "originalFilename":"contrat.pdf","contentType":"application/pdf","sizeBytes":2048,
             "storagePath":"/tmp/contrat.pdf","occurredAt":"2026-07-05T10:00:00"}
            """;

        consumer.onDocumentEvent(payload);

        verify(documentReadyNotificationService, never()).notifyClientDocumentReady(any());
    }

    @Test
    @DisplayName("JSON malformé - ne plante jamais")
    void onDocumentEvent_malformedJson_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onDocumentEvent("pas du JSON"));
        verify(documentReadyNotificationService, never()).notifyClientDocumentReady(any());
    }
}