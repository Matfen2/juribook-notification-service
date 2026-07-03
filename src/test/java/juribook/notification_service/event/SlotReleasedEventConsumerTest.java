package juribook.notification_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.notification_service.service.SlotReleaseNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests du consumer slot-events.
 *
 * ⚠️ SlotReleasedEventConsumer et n'a jamais été
 * revu en détail depuis dans cette conversation, ce test est
 * reconstruit à partir du comportement documenté dans le README
 * (SlotReleaseNotificationService.notifyWaitlistedClients(lawyerId, slotId)).
 * Vérifie la signature exacte de cette méthode dans ton fichier réel
 * avant de faire confiance à ce test, adapte le nom de méthode/l'ordre
 * des paramètres si besoin.
 */
@ExtendWith(MockitoExtension.class)
class SlotReleasedEventConsumerTest {

    @Mock
    private SlotReleaseNotificationService slotReleaseNotificationService;

    private SlotReleasedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new SlotReleasedEventConsumer(objectMapper, slotReleaseNotificationService);
    }

    @Test
    void onSlotEvent_slotReleased_callsNotifyWaitlistedClients() {
        String payload = """
            {"eventType":"slot.released","lawyerId":1,"slotId":67,"occurredAt":"2026-07-01T12:25:10.045"}
            """;

        consumer.onSlotEvent(payload);

        verify(slotReleaseNotificationService).notifyWaitlistedClients(1L, 67L);
    }

    @Test
    void onSlotEvent_unknownEventType_doesNotNotify() {
        String payload = """
            {"eventType":"slot.something-else","lawyerId":1,"slotId":67,"occurredAt":"2026-07-01T12:25:10.045"}
            """;

        consumer.onSlotEvent(payload);

        verify(slotReleaseNotificationService, never()).notifyWaitlistedClients(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void onSlotEvent_malformedJson_doesNotThrow() {
        assertDoesNotThrow(() -> consumer.onSlotEvent("pas du JSON du tout"));

        verify(slotReleaseNotificationService, never()).notifyWaitlistedClients(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }
}