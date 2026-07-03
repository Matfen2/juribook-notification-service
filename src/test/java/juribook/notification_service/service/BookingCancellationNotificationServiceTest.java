package juribook.notification_service.service;

import juribook.notification_service.client.*;
import juribook.notification_service.event.BookingEvent;
import juribook.notification_service.notification.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Tests de BookingCancellationNotificationService.
 *
 * Particularité par rapport aux 3 autres orchestrateurs : tolère un
 * détail de créneau manquant (getBookingDetails vide) plutôt que
 * d'abandonner l'envoi, c'est le seul cas testé ici qui diffère du
 * pattern "toute résolution manquante = on n'envoie rien".
 */
@ExtendWith(MockitoExtension.class)
class BookingCancellationNotificationServiceTest {

    @Mock private BookingServiceClient bookingServiceClient;
    @Mock private LawyerServiceClient lawyerServiceClient;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private NotificationSender notificationSender;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private BookingCancellationNotificationService service;

    private BookingEvent event;
    private BookingDetailsDto details;
    private LawyerProfileDto lawyer;
    private UserContactDto client;

    @BeforeEach
    void setUp() {
        event = new BookingEvent("booking.cancelled", 1L, 42L, 4L, 15L, "CANCELLED", "Litige", LocalDateTime.now());
        details = new BookingDetailsDto(1L, 4L, 15L, "CANCELLED", "Litige",
                LocalDate.of(2026, 7, 6), LocalTime.of(9, 0), LocalTime.of(9, 30), LocalDateTime.now());
        lawyer = new LawyerProfileDto(4L, 55L, "Sophie Martin");
        client = new UserContactDto(42L, "Jean Dupont", "jean@test.com");
    }

    @Test
    void notifyClientOfCancellation_happyPath_sendsEmailWithSchedule() {
        when(lawyerServiceClient.getLawyer(4L)).thenReturn(Optional.of(lawyer));
        when(authServiceClient.getContact(42L)).thenReturn(Optional.of(client));
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.of(details));

        service.notifyClientOfCancellation(event);

        verify(notificationSender).sendCancellationEmail(
                "jean@test.com", "Jean Dupont", "Sophie Martin",
                LocalDate.of(2026, 7, 6), LocalTime.of(9, 0), LocalTime.of(9, 30));
        verify(notificationService).createNotification(eq(42L), any(), any(), eq(1L));
    }

    @Test
    void notifyClientOfCancellation_bookingDetailsMissing_stillSendsEmail_withNullSchedule() {
        // Contrairement aux 3 autres orchestrateurs : un détail de
        // créneau manquant ne bloque PAS l'envoi ici (le créneau a pu
        // être déjà libéré/repris avant que ce handler ne s'exécute).
        when(lawyerServiceClient.getLawyer(4L)).thenReturn(Optional.of(lawyer));
        when(authServiceClient.getContact(42L)).thenReturn(Optional.of(client));
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.empty());

        service.notifyClientOfCancellation(event);

        verify(notificationSender).sendCancellationEmail(
                eq("jean@test.com"), eq("Jean Dupont"), eq("Sophie Martin"),
                isNull(), isNull(), isNull());
        verify(notificationService).createNotification(eq(42L), any(), any(), eq(1L));
    }

    @Test
    void notifyClientOfCancellation_missingLawyer_sendsNothing() {
        when(lawyerServiceClient.getLawyer(4L)).thenReturn(Optional.empty());

        service.notifyClientOfCancellation(event);

        verifyNoInteractions(notificationSender, notificationService);
        verifyNoInteractions(authServiceClient, bookingServiceClient);
    }

    @Test
    void notifyClientOfCancellation_missingClient_sendsNothing() {
        when(lawyerServiceClient.getLawyer(4L)).thenReturn(Optional.of(lawyer));
        when(authServiceClient.getContact(42L)).thenReturn(Optional.empty());

        service.notifyClientOfCancellation(event);

        verifyNoInteractions(notificationSender, notificationService);
        // Le détail de réservation n'est même pas consulté si le client
        // n'est pas résolvable — pas la peine.
        verifyNoInteractions(bookingServiceClient);
    }
}