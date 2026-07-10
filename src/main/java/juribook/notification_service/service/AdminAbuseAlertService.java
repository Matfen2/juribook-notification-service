package juribook.notification_service.service;

import juribook.notification_service.event.AbuseEvent;
import juribook.notification_service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Alerte l'admin par email quand un abus est détecté.
 *
 * Adresse admin configurable (app.admin-email) plutôt qu'un annuaire
 * dynamique des comptes ADMIN, un seul compte admin existe dans ce
 * projet, une constante suffit.
 * Pas de notification in-app : aucune page admin avec cloche de
 * notification n'existe côté frontend, en construire une pour ce seul
 * sprint aurait été disproportionné, email uniquement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAbuseAlertService {

    private final NotificationSender notificationSender;

    @Value("${app.admin-email}")
    private String adminEmail;

    public void alertAdmin(AbuseEvent event) {
        notificationSender.sendAbuseAlertEmail(
                adminEmail,
                event.actorId(),
                event.reason(),
                event.signalCount(),
                event.occurredAt()
        );

        log.info("Admin alerté : actorId={}, reason={}, signalCount={}",
                event.actorId(), event.reason(), event.signalCount());
    }
}