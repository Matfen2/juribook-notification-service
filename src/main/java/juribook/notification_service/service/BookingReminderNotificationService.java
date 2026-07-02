package juribook.notification_service.service;

import juribook.notification_service.client.AuthServiceClient;
import juribook.notification_service.client.BookingDetailsDto;
import juribook.notification_service.client.BookingServiceClient;
import juribook.notification_service.client.LawyerProfileDto;
import juribook.notification_service.client.LawyerServiceClient;
import juribook.notification_service.client.UserContactDto;
import juribook.notification_service.entity.NotificationType;
import juribook.notification_service.event.BookingEvent;
import juribook.notification_service.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingReminderNotificationService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingServiceClient bookingServiceClient;
    private final LawyerServiceClient lawyerServiceClient;
    private final AuthServiceClient authServiceClient;
    private final NotificationSender notificationSender;
    private final NotificationService notificationService;

    public void notifyClientOfReminder(BookingEvent event) {
        Optional<BookingDetailsDto> details = bookingServiceClient.getBookingDetails(event.bookingId());
        if (details.isEmpty() || details.get().date() == null || details.get().startTime() == null) {
            log.warn("Détail de créneau introuvable pour bookingId={}, rappel non envoyé", event.bookingId());
            return;
        }

        Optional<LawyerProfileDto> lawyer = lawyerServiceClient.getLawyer(event.lawyerId());
        if (lawyer.isEmpty()) {
            log.warn("Avocat introuvable (lawyerId={}) pour bookingId={}, rappel non envoyé",
                    event.lawyerId(), event.bookingId());
            return;
        }

        Optional<UserContactDto> client = authServiceClient.getContact(event.clientId());
        if (client.isEmpty()) {
            log.warn("Client introuvable (clientId={}) pour bookingId={}, rappel non envoyé",
                    event.clientId(), event.bookingId());
            return;
        }

        BookingDetailsDto d = details.get();

        notificationSender.sendReminderEmail(
                client.get().email(), client.get().name(), lawyer.get().name(),
                d.date(), d.startTime(), d.endTime()
        );

        // Sprint 5.6 : notification in-app.
        String message = "Rappel : rendez-vous avec %s demain le %s à %s".formatted(
                lawyer.get().name(), d.date().format(DATE_FORMATTER), d.startTime().format(TIME_FORMATTER));
        notificationService.createNotification(
                event.clientId(), NotificationType.BOOKING_REMINDER, message, event.bookingId());
    }
}