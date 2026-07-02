package juribook.notification_service.service;

import juribook.notification_service.dto.response.NotificationResponse;
import juribook.notification_service.entity.Notification;
import juribook.notification_service.entity.NotificationType;
import juribook.notification_service.exception.NotificationNotFoundException;
import juribook.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des notifications in-app.
 *
 * createNotification est appelé par les orchestrateurs existants
 * (BookingConfirmationNotificationService, BookingRequestNotificationService,
 * BookingReminderNotificationService) juste après l'envoi de l'email
 * correspondant, même déclencheur, deux canaux.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createNotification(Long recipientAuthUserId, NotificationType type, String message, Long bookingId) {
        Notification notification = new Notification();
        notification.setRecipientAuthUserId(recipientAuthUserId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setBookingId(bookingId);

        notificationRepository.save(notification);
        log.info("Notification in-app créée : recipientAuthUserId={}, type={}", recipientAuthUserId, type);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long authUserId) {
        return notificationRepository.findByRecipientAuthUserIdOrderByCreatedAtDesc(authUserId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long authUserId) {
        return notificationRepository.countByRecipientAuthUserIdAndReadFalse(authUserId);
    }

    @Transactional
    public void markAsRead(Long authUserId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(
                    "Notification introuvable : id=" + notificationId));

        if (!notification.getRecipientAuthUserId().equals(authUserId)) {
            throw new AccessDeniedException("Cette notification n'appartient pas à cet utilisateur");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }
}