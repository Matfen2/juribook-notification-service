package juribook.notification_service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub d'envoi d'email, logue l'intention d'envoi plutôt que d'envoyer
 * réellement un email.
 *
 * Pour brancher un vrai envoi plus tard : injecter JavaMailSender
 * (spring-boot-starter-mail est déjà dans le pom.xml), configurer un
 * vrai provider SMTP dans application.yaml (spring.mail.*), et résoudre
 * l'adresse email du client, ce qui nécessite un appel à l'auth-service
 * (clientId seul ne suffit pas, cf. limite ci-dessous).
 *
 * ⚠️ Limite connue : l'adresse email réelle du client n'est jamais
 * résolue ici (pas d'appel à l'auth-service pour ce sprint). Seul le
 * clientId est disponible et loggé.
 */
@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    @Override
    public void sendSlotReleasedNotification(Long clientId, Long lawyerId, Long slotId) {
        log.info("[EMAIL STUB] Notification créneau libéré → clientId={}, lawyerId={}, slotId={}. "
                + "Un email serait envoyé ici en production.", clientId, lawyerId, slotId);
    }
}