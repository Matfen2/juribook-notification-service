package juribook.notification_service.service;

import juribook.notification_service.client.AuthServiceClient;
import juribook.notification_service.client.LawyerProfileDto;
import juribook.notification_service.client.LawyerServiceClient;
import juribook.notification_service.client.UserContactDto;
import juribook.notification_service.entity.NotificationType;
import juribook.notification_service.event.DocumentEvent;
import juribook.notification_service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestration de la notification "document prêt" (Sprint 6.7).
 *
 * Plus légère que les orchestrateurs booking.* : pas besoin de
 * BookingServiceClient ici, le nom du fichier vient déjà directement
 * de l'événement — seuls le nom de l'avocat et le contact du client
 * doivent être résolus.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentReadyNotificationService {

    private final LawyerServiceClient lawyerServiceClient;
    private final AuthServiceClient authServiceClient;
    private final NotificationSender notificationSender;
    private final NotificationService notificationService;

    public void notifyClientDocumentReady(DocumentEvent event) {
        Optional<LawyerProfileDto> lawyer = lawyerServiceClient.getLawyer(event.lawyerId());
        if (lawyer.isEmpty()) {
            log.warn("Avocat introuvable (lawyerId={}) pour documentId={}, notification non envoyée",
                    event.lawyerId(), event.documentId());
            return;
        }

        Optional<UserContactDto> client = authServiceClient.getContact(event.clientId());
        if (client.isEmpty()) {
            log.warn("Client introuvable (clientId={}) pour documentId={}, notification non envoyée",
                    event.clientId(), event.documentId());
            return;
        }

        notificationSender.sendDocumentReadyEmail(
                client.get().email(),
                client.get().name(),
                lawyer.get().name(),
                event.originalFilename()
        );

        String message = "Votre document \"%s\" a bien été reçu par %s".formatted(
                event.originalFilename(), lawyer.get().name());
        notificationService.createNotification(
                event.clientId(), NotificationType.DOCUMENT_READY, message, event.bookingId());
    }
}