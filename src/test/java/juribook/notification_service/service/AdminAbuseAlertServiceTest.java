package juribook.notification_service.service;

import juribook.notification_service.event.AbuseEvent;
import juribook.notification_service.notification.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAbuseAlertServiceTest {

    @Mock
    private NotificationSender notificationSender;

    private AdminAbuseAlertService adminAbuseAlertService;

    @BeforeEach
    void setUp() {
        adminAbuseAlertService = new AdminAbuseAlertService(notificationSender);
        ReflectionTestUtils.setField(adminAbuseAlertService, "adminEmail", "admin@juribook.fr");
    }

    @Test
    @DisplayName("transmet fidèlement tous les détails de l'événement à l'email admin")
    void alertAdmin_forwardsAllEventDetailsToEmail() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 5, 10, 0);
        AbuseEvent event = new AbuseEvent("abuse.detected", 4L,
                "Plus de 5 annulations en 7 jours", 6L, occurredAt);

        adminAbuseAlertService.alertAdmin(event);

        verify(notificationSender).sendAbuseAlertEmail(
                "admin@juribook.fr", 4L, "Plus de 5 annulations en 7 jours", 6L, occurredAt);
    }
}