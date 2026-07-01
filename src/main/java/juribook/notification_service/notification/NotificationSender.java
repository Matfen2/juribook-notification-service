package juribook.notification_service.notification;

/**
 * Abstraction de l'envoi de notification à un client.
 *
 * Une seule implémentation pour ce sprint : EmailNotificationSender,
 * volontairement un stub qui logue plutôt que d'envoyer un vrai email
 * (décision explicite pour ce sprint, pas de provider SMTP configuré).
 * La notification in-app (persistée, consultable via API par le
 * frontend) est repoussée à un sprint dédié.
 */
public interface NotificationSender {

    void sendSlotReleasedNotification(Long clientId, Long lawyerId, Long slotId);
}