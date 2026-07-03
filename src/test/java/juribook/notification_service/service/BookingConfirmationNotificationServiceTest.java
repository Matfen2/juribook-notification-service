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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingConfirmationNotificationServiceTest {

    @Mock private BookingServiceClient bookingServiceClient;
    @Mock private LawyerServiceClient lawyerServiceClient;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private NotificationSender notificationSender;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private BookingConfirmationNotificationService service;

    private BookingEvent event;
    private BookingDetailsDto details;
    private LawyerProfileDto lawyer;
    private UserContactDto client;

    @BeforeEach
    void setUp() {
        event = new BookingEvent("booking.confirmed", 1L, 42L, 4L, 15L, "CONFIRMED", "Litige", LocalDateTime.now());
        details = new BookingDetailsDto(1L, 4L, 15L, "CONFIRMED", "Litige",
                LocalDate.of(2026, 7, 6), LocalTime.of(9, 0), LocalTime.of(9, 30), LocalDateTime.now());
        lawyer = new LawyerProfileDto(4L, 55L, "Sophie Martin");
        client = new UserContactDto(42L, "Jean Dupont", "jean@test.com");
    }

    @Test
    void notifyClientOfConfirmation_happyPath_sendsEmailAndCreatesNotification() {
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.of(details));
        when(lawyerServiceClient.getLawyer(4L)).thenReturn(Optional.of(lawyer));
        when(authServiceClient.getContact(42L)).thenReturn(Optional.of(client));

        service.notifyClientOfConfirmation(event);

        verify(notificationSender).sendBookingConfirmedEmail(
                "jean@test.com", "Jean Dupont", "Sophie Martin",
                LocalDate.of(2026, 7, 6), LocalTime.of(9, 0), LocalTime.of(9, 30));
        verify(notificationService).createNotification(eq(42L), any(), any(), eq(1L));
    }

    @Test
    void notifyClientOfConfirmation_missingBookingDetails_sendsNothing() {
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.empty());

        service.notifyClientOfConfirmation(event);

        verifyNoInteractions(notificationSender, notificationService);
        verifyNoInteractions(lawyerServiceClient, authServiceClient);
    }

    @Test
    void notifyClientOfConfirmation_bookingDetailsMissingDate_sendsNothing() {
        BookingDetailsDto incomplete = new BookingDetailsDto(1L, 4L, 15L, "CONFIRMED", "Litige",
                null, null, null, LocalDateTime.now());
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.of(incomplete));

        service.notifyClientOfConfirmation(event);

        verifyNoInteractions(notificationSender, notificationService);
    }

    @Test
    void notifyClientOfConfirmation_missingLawyer_sendsNothing() {
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.of(details));
        when(lawyerServiceClient.getLawyer(4L)).thenReturn(Optional.empty());

        service.notifyClientOfConfirmation(event);

        verifyNoInteractions(notificationSender, notificationService);
        verifyNoInteractions(authServiceClient);
    }

    @Test
    void notifyClientOfConfirmation_missingClientContact_sendsNothing() {
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.of(details));
        when(lawyerServiceClient.getLawyer(4L)).thenReturn(Optional.of(lawyer));
        when(authServiceClient.getContact(42L)).thenReturn(Optional.empty());

        service.notifyClientOfConfirmation(event);

        verifyNoInteractions(notificationSender, notificationService);
    }
}