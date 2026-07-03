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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingRequestNotificationServiceTest {

    @Mock private BookingServiceClient bookingServiceClient;
    @Mock private LawyerServiceClient lawyerServiceClient;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private NotificationSender notificationSender;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private BookingRequestNotificationService service;

    private static final Long LAWYER_ID = 4L;
    private static final Long LAWYER_AUTH_USER_ID = 55L;
    private static final Long CLIENT_ID = 42L;

    private BookingEvent event;
    private BookingDetailsDto details;
    private LawyerProfileDto lawyer;
    private UserContactDto lawyerContact;
    private UserContactDto clientContact;

    @BeforeEach
    void setUp() {
        event = new BookingEvent("booking.created", 1L, CLIENT_ID, LAWYER_ID, 15L, "PENDING", "Litige", LocalDateTime.now());
        details = new BookingDetailsDto(1L, LAWYER_ID, 15L, "PENDING", "Litige",
                LocalDate.of(2026, 7, 6), LocalTime.of(9, 0), LocalTime.of(9, 30), LocalDateTime.now());
        lawyer = new LawyerProfileDto(LAWYER_ID, LAWYER_AUTH_USER_ID, "Sophie Martin");
        lawyerContact = new UserContactDto(LAWYER_AUTH_USER_ID, "Sophie Martin", "sophie@test.com");
        clientContact = new UserContactDto(CLIENT_ID, "Jean Dupont", "jean@test.com");
    }

    @Test
    void notifyLawyerOfNewRequest_happyPath_resolvesAuthUserId_sendsEmailToLawyer() {
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.of(details));
        when(lawyerServiceClient.getLawyer(LAWYER_ID)).thenReturn(Optional.of(lawyer));
        when(authServiceClient.getContact(LAWYER_AUTH_USER_ID)).thenReturn(Optional.of(lawyerContact));
        when(authServiceClient.getContact(CLIENT_ID)).thenReturn(Optional.of(clientContact));

        service.notifyLawyerOfNewRequest(event);

        // Le mail part bien à l'adresse de l'avocat (résolue via
        // authUserId), pas via lawyerId directement.
        verify(notificationSender).sendNewBookingRequestEmail(
                eq("sophie@test.com"), eq("Sophie Martin"), eq("Jean Dupont"),
                any(), any(), any(), any());

        // Notification in-app rattachée à l'authUserId de l'avocat, pas
        // à son lawyerId (cf. commentaire de la classe testée).
        verify(notificationService).createNotification(eq(LAWYER_AUTH_USER_ID), any(), any(), eq(1L));
    }

    @Test
    void notifyLawyerOfNewRequest_lawyerFoundButAuthUserIdNull_sendsNothing() {
        LawyerProfileDto lawyerWithoutAuthUserId = new LawyerProfileDto(LAWYER_ID, null, "Sophie Martin");
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.of(details));
        when(lawyerServiceClient.getLawyer(LAWYER_ID)).thenReturn(Optional.of(lawyerWithoutAuthUserId));

        service.notifyLawyerOfNewRequest(event);

        verifyNoInteractions(notificationSender, notificationService);
        verifyNoInteractions(authServiceClient);
    }

    @Test
    void notifyLawyerOfNewRequest_lawyerContactMissing_sendsNothing() {
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.of(details));
        when(lawyerServiceClient.getLawyer(LAWYER_ID)).thenReturn(Optional.of(lawyer));
        when(authServiceClient.getContact(LAWYER_AUTH_USER_ID)).thenReturn(Optional.empty());

        service.notifyLawyerOfNewRequest(event);

        verifyNoInteractions(notificationSender, notificationService);
        // Le contact CLIENT n'est jamais résolu si le contact avocat
        // échoue déjà, pas d'appel superflu.
        verify(authServiceClient, never()).getContact(CLIENT_ID);
    }

    @Test
    void notifyLawyerOfNewRequest_clientContactMissing_sendsNothing() {
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.of(details));
        when(lawyerServiceClient.getLawyer(LAWYER_ID)).thenReturn(Optional.of(lawyer));
        when(authServiceClient.getContact(LAWYER_AUTH_USER_ID)).thenReturn(Optional.of(lawyerContact));
        when(authServiceClient.getContact(CLIENT_ID)).thenReturn(Optional.empty());

        service.notifyLawyerOfNewRequest(event);

        verifyNoInteractions(notificationSender, notificationService);
    }

    @Test
    void notifyLawyerOfNewRequest_missingBookingDetails_sendsNothing() {
        when(bookingServiceClient.getBookingDetails(1L)).thenReturn(Optional.empty());

        service.notifyLawyerOfNewRequest(event);

        verifyNoInteractions(notificationSender, notificationService, lawyerServiceClient, authServiceClient);
    }
}