package juribook.notification_service.service;

import juribook.notification_service.client.BookingServiceClient;
import juribook.notification_service.client.WaitlistEntryDto;
import juribook.notification_service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestration de la notification des clients en liste d'attente
 * suite à un événement slot.released.
 *
 * Ne modifie jamais l'état de la liste d'attente côté booking-service
 * (aucune suppression des entrées après notification), le booking-service
 * reste seul propriétaire de ces données.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlotReleaseNotificationService {

    private final BookingServiceClient bookingServiceClient;
    private final NotificationSender notificationSender;

    public void notifyWaitlistedClients(Long lawyerId, Long slotId) {
        List<WaitlistEntryDto> waitlist = bookingServiceClient.getWaitlist(lawyerId);

        if (waitlist.isEmpty()) {
            log.debug("Aucun client en liste d'attente pour lawyerId={}, rien à notifier", lawyerId);
            return;
        }

        log.info("Notification de {} client(s) en attente pour lawyerId={} suite à la libération du créneau {}",
                waitlist.size(), lawyerId, slotId);

        waitlist.forEach(entry ->
            notificationSender.sendSlotReleasedNotification(entry.clientId(), lawyerId, slotId));
    }
}