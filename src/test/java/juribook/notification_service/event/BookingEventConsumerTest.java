package juribook.notification_service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import juribook.notification_service.service.BookingCancellationNotificationService;
import juribook.notification_service.service.BookingConfirmationNotificationService;
import juribook.notification_service.service.BookingReminderNotificationService;
import juribook.notification_service.service.BookingRequestNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests du routage de BookingEventConsumer.
 *
 * Kafka lui-même n'est jamais impliqué, on appelle directement
 * onBookingEvent(String) avec un payload JSON brut, exactement comme le
 * ferait le conteneur Spring Kafka après désérialisation du message.
 * Les 4 orchestrateurs sont mockés : on vérifie uniquement que le bon
 * est appelé pour le bon eventType, pas leur comportement interne
 * (couvert par leurs propres tests dédiés).
 *
 * @InjectMocks n'est volontairement pas utilisé ici : ObjectMapper n'est
 * pas un mock (on veut une vraie désérialisation JSON), la construction
 * manuelle du consumer est plus explicite et plus fiable que de compter
 * sur le fallback d'injection par champ de Mockito.
 */
@ExtendWith(MockitoExtension.class)
class BookingEventConsumerTest {

    @Mock
    private BookingConfirmationNotificationService confirmationNotificationService;

    @Mock
    private BookingRequestNotificationService requestNotificationService;

    @Mock
    private BookingReminderNotificationService reminderNotificationService;

    @Mock
    private BookingCancellationNotificationService cancellationNotificationService;

    private BookingEventConsumer consumer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        consumer = new BookingEventConsumer(
                objectMapper,
                confirmationNotificationService,
                requestNotificationService,
                reminderNotificationService,
                cancellationNotificationService
        );
    }

    private String eventJson(String eventType, long bookingId, long clientId, long lawyerId) {
        return """
            {"eventType":"%s","bookingId":%d,"clientId":%d,"lawyerId":%d,"timeSlotId":1,"status":"PENDING","reason":"test","occurredAt":"2026-07-01T10:00:00"}
            """.formatted(eventType, bookingId, clientId, lawyerId);
    }

    @Test
    void onBookingEvent_routesBookingCreated_toRequestNotificationService() {
        consumer.onBookingEvent(eventJson("booking.created", 1, 2, 3));

        ArgumentCaptor<BookingEvent> captor = ArgumentCaptor.forClass(BookingEvent.class);
        verify(requestNotificationService).notifyLawyerOfNewRequest(captor.capture());
        assertThat(captor.getValue().bookingId()).isEqualTo(1L);

        verify(confirmationNotificationService, never()).notifyClientOfConfirmation(any());
        verify(reminderNotificationService, never()).notifyClientOfReminder(any());
        verify(cancellationNotificationService, never()).notifyClientOfCancellation(any());
    }

    @Test
    void onBookingEvent_routesBookingConfirmed_toConfirmationNotificationService() {
        consumer.onBookingEvent(eventJson("booking.confirmed", 1, 2, 3));

        verify(confirmationNotificationService).notifyClientOfConfirmation(any());
        verify(requestNotificationService, never()).notifyLawyerOfNewRequest(any());
    }

    @Test
    void onBookingEvent_routesBookingCancelled_toCancellationNotificationService() {
        consumer.onBookingEvent(eventJson("booking.cancelled", 1, 2, 3));

        verify(cancellationNotificationService).notifyClientOfCancellation(any());
    }

    @Test
    void onBookingEvent_routesBookingReminder_toReminderNotificationService() {
        consumer.onBookingEvent(eventJson("booking.reminder", 1, 2, 3));

        verify(reminderNotificationService).notifyClientOfReminder(any());
    }

    @Test
    void onBookingEvent_ignoresUnknownEventType_callsNoOrchestrator() {
        consumer.onBookingEvent(eventJson("booking.something-unexpected", 1, 2, 3));

        verify(confirmationNotificationService, never()).notifyClientOfConfirmation(any());
        verify(requestNotificationService, never()).notifyLawyerOfNewRequest(any());
        verify(reminderNotificationService, never()).notifyClientOfReminder(any());
        verify(cancellationNotificationService, never()).notifyClientOfCancellation(any());
    }

    @Test
    void onBookingEvent_malformedJson_doesNotThrow_callsNoOrchestrator() {
        // Ne doit jamais lever d'exception, un message illisible est
        // loggué et ignoré, pas de crash du listener Kafka.
        assertDoesNotThrow(() -> consumer.onBookingEvent("{ceci n'est pas du JSON valide"));

        verify(confirmationNotificationService, never()).notifyClientOfConfirmation(any());
        verify(requestNotificationService, never()).notifyLawyerOfNewRequest(any());
    }
}