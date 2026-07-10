package juribook.notification_service.service;

import juribook.notification_service.client.AuthServiceClient;
import juribook.notification_service.client.LawyerProfileDto;
import juribook.notification_service.client.LawyerServiceClient;
import juribook.notification_service.client.UserContactDto;
import juribook.notification_service.entity.NotificationType;
import juribook.notification_service.event.DocumentEvent;
import juribook.notification_service.notification.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentReadyNotificationServiceTest {

    @Mock private LawyerServiceClient lawyerServiceClient;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private NotificationSender notificationSender;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private DocumentReadyNotificationService documentReadyNotificationService;

    private DocumentEvent event;
    private LawyerProfileDto lawyer;
    private UserContactDto client;

    @BeforeEach
    void setUp() {
        event = new DocumentEvent("document.ready", 1L, 7L, 42L, 4L,
                "contrat.pdf", "application/pdf", 2048, "/permanent/contrat.pdf", LocalDateTime.now());
        lawyer = new LawyerProfileDto(4L, 55L, "Sophie Martin");
        client = new UserContactDto(42L, "Jean Dupont", "jean@test.com");
    }

    @Test
    @DisplayName("cas nominal - email envoyé et notification in-app créée")
    void notifyClientDocumentReady_happyPath_sendsEmailAndCreatesNotification() {
        when(lawyerServiceClient.getLawyer(4L)).thenReturn(Optional.of(lawyer));
        when(authServiceClient.getContact(42L)).thenReturn(Optional.of(client));

        documentReadyNotificationService.notifyClientDocumentReady(event);

        verify(notificationSender).sendDocumentReadyEmail(
                "jean@test.com", "Jean Dupont", "Sophie Martin", "contrat.pdf");
        verify(notificationService).createNotification(
                eq(42L), eq(NotificationType.DOCUMENT_READY), any(), eq(7L));
    }

    @Test
    @DisplayName("avocat introuvable - n'envoie rien")
    void notifyClientDocumentReady_lawyerNotFound_sendsNothing() {
        when(lawyerServiceClient.getLawyer(4L)).thenReturn(Optional.empty());

        documentReadyNotificationService.notifyClientDocumentReady(event);

        verifyNoInteractions(notificationSender, notificationService);
        verifyNoInteractions(authServiceClient);
    }

    @Test
    @DisplayName("client introuvable - n'envoie rien")
    void notifyClientDocumentReady_clientNotFound_sendsNothing() {
        when(lawyerServiceClient.getLawyer(4L)).thenReturn(Optional.of(lawyer));
        when(authServiceClient.getContact(42L)).thenReturn(Optional.empty());

        documentReadyNotificationService.notifyClientDocumentReady(event);

        verifyNoInteractions(notificationSender, notificationService);
    }
}