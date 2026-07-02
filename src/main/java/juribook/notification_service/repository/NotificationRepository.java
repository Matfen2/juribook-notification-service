package juribook.notification_service.repository;

import juribook.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientAuthUserIdOrderByCreatedAtDesc(Long recipientAuthUserId);

    long countByRecipientAuthUserIdAndReadFalse(Long recipientAuthUserId);
}