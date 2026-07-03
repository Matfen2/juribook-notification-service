package juribook.notification_service.service;

import juribook.notification_service.dto.response.NotificationResponse;
import juribook.notification_service.entity.Notification;
import juribook.notification_service.entity.NotificationType;
import juribook.notification_service.exception.NotificationNotFoundException;
import juribook.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private static final Long RECIPIENT_ID = 42L;

    @Test
    void createNotification_persistsWithCorrectFields() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.createNotification(RECIPIENT_ID, NotificationType.BOOKING_CONFIRMED, "Message test", 7L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getRecipientAuthUserId()).isEqualTo(RECIPIENT_ID);
        assertThat(saved.getType()).isEqualTo(NotificationType.BOOKING_CONFIRMED);
        assertThat(saved.getMessage()).isEqualTo("Message test");
        assertThat(saved.getBookingId()).isEqualTo(7L);
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    void getMyNotifications_returnsEmptyList_whenNoneExist() {
        when(notificationRepository.findByRecipientAuthUserIdOrderByCreatedAtDesc(RECIPIENT_ID))
                .thenReturn(List.of());

        List<NotificationResponse> result = notificationService.getMyNotifications(RECIPIENT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void getMyNotifications_mapsEntitiesToResponses() {
        Notification n = buildNotification(1L, RECIPIENT_ID, false);
        when(notificationRepository.findByRecipientAuthUserIdOrderByCreatedAtDesc(RECIPIENT_ID))
                .thenReturn(List.of(n));

        List<NotificationResponse> result = notificationService.getMyNotifications(RECIPIENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).type()).isEqualTo("BOOKING_CONFIRMED");
        assertThat(result.get(0).read()).isFalse();
    }

    @Test
    void getUnreadCount_delegatesToRepository() {
        when(notificationRepository.countByRecipientAuthUserIdAndReadFalse(RECIPIENT_ID)).thenReturn(3L);

        long count = notificationService.getUnreadCount(RECIPIENT_ID);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void markAsRead_ownNotification_marksAsReadAndSaves() {
        Notification n = buildNotification(1L, RECIPIENT_ID, false);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(RECIPIENT_ID, 1L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().isRead()).isTrue();
    }

    @Test
    void markAsRead_notificationNotFound_throwsNotificationNotFoundException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(RECIPIENT_ID, 999L))
                .isInstanceOf(NotificationNotFoundException.class);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_belongsToAnotherUser_throwsAccessDeniedException() {
        Notification n = buildNotification(1L, 999L, false); // appartient à un autre utilisateur
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.markAsRead(RECIPIENT_ID, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(notificationRepository, never()).save(any());
    }

    private Notification buildNotification(Long id, Long recipientAuthUserId, boolean read) {
        Notification n = new Notification();
        n.setId(id);
        n.setRecipientAuthUserId(recipientAuthUserId);
        n.setType(NotificationType.BOOKING_CONFIRMED);
        n.setMessage("Test");
        n.setBookingId(7L);
        n.setRead(read);
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }
}