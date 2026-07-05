package juribook.notification_service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private static final String FROM_ADDRESS = "no-reply@juribook.fr";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;

    // Injection du JavaMailSender via le constructeur (Spring Boot auto-configure un bean JavaMailSender si les propriétés mail sont définies).
    public EmailNotificationSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Email au client quand un créneau qu'il attendait est libéré par un autre client.
    @Override
    public void sendSlotReleasedNotification(Long clientId, Long lawyerId, Long slotId) {
        log.info("[EMAIL STUB] Notification créneau libéré → clientId={}, lawyerId={}, slotId={}. "
                + "Un email serait envoyé ici en production.", clientId, lawyerId, slotId);
    }

    // Email au client quand sa réservation a été confirmée par l'avocat.
    @Override
    public void sendBookingConfirmedEmail(String toEmail, String clientName, String lawyerName,
                                           LocalDate date, LocalTime startTime, LocalTime endTime) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        message.setTo(toEmail);
        message.setSubject("Votre rendez-vous avec " + lawyerName + " est confirmé");
        message.setText("""
                Bonjour %s,

                Votre rendez-vous avec %s est confirmé.

                Date : %s
                Heure : %s - %s

                À bientôt sur JuriBook.
                """.formatted(
                clientName, lawyerName,
                date.format(DATE_FORMATTER), startTime.format(TIME_FORMATTER), endTime.format(TIME_FORMATTER)
        ));

        send(message, "confirmation");
    }

    // Email à l'avocat quand un client lui envoie une demande de réservation.
    @Override
    public void sendNewBookingRequestEmail(String toEmail, String lawyerName, String clientName,
                                            LocalDate date, LocalTime startTime, LocalTime endTime,
                                            String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        message.setTo(toEmail);
        message.setSubject("Nouvelle demande de rendez-vous de " + clientName);
        message.setText("""
                Bonjour %s,

                Vous avez reçu une nouvelle demande de rendez-vous de la part de %s.

                Date souhaitée : %s
                Heure : %s - %s
                Motif : %s

                Connectez-vous à votre espace JuriBook pour confirmer ou refuser cette demande.

                À bientôt sur JuriBook.
                """.formatted(
                lawyerName, clientName,
                date.format(DATE_FORMATTER), startTime.format(TIME_FORMATTER), endTime.format(TIME_FORMATTER),
                reason != null && !reason.isBlank() ? reason : "Non renseigné"
        ));

        send(message, "nouvelle demande");
    }

    // Email au client quand sa réservation est annulée par l'avocat.
    @Override
    public void sendReminderEmail(String toEmail, String clientName, String lawyerName,
                                   LocalDate date, LocalTime startTime, LocalTime endTime) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        message.setTo(toEmail);
        message.setSubject("Rappel : votre rendez-vous avec " + lawyerName + " demain");
        message.setText("""
                Bonjour %s,

                Petit rappel : votre rendez-vous avec %s a lieu demain.

                Date : %s
                Heure : %s - %s

                À bientôt sur JuriBook.
                """.formatted(
                clientName, lawyerName,
                date.format(DATE_FORMATTER), startTime.format(TIME_FORMATTER), endTime.format(TIME_FORMATTER)
        ));

        send(message, "rappel 24h");
    }

    // Email au client quand sa réservation est annulée par l'avocat.
    @Override
    public void sendCancellationEmail(String toEmail, String clientName, String lawyerName,
                                       LocalDate date, LocalTime startTime, LocalTime endTime) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        message.setTo(toEmail);
        message.setSubject("Votre rendez-vous avec " + lawyerName + " a été annulé");

        String scheduleLine = (date != null && startTime != null && endTime != null)
                ? "Il était prévu le %s de %s à %s.".formatted(
                    date.format(DATE_FORMATTER), startTime.format(TIME_FORMATTER), endTime.format(TIME_FORMATTER))
                : "";

        message.setText("""
                Bonjour %s,

                Votre rendez-vous avec %s a été annulé.
                %s

                N'hésitez pas à consulter les disponibilités de cet avocat, ou d'un autre, sur JuriBook.

                À bientôt sur JuriBook.
                """.formatted(clientName, lawyerName, scheduleLine));

        send(message, "annulation");
    }

    // Email au client quand son document uploadé a été traité.
    @Override
    public void sendDocumentReadyEmail(String toEmail, String clientName, String lawyerName, String filename) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        message.setTo(toEmail);
        message.setSubject("Votre document a bien été reçu");
        message.setText("""
                Bonjour %s,

                Le document que vous avez envoyé à %s a bien été reçu et traité :
                « %s »

                À bientôt sur JuriBook.
                """.formatted(clientName, lawyerName, filename));

        send(message, "document prêt");
    }

    // Méthode utilitaire pour envoyer un email et gérer les exceptions.
    private void send(SimpleMailMessage message, String emailKind) {
        try {
            mailSender.send(message);
            log.info("Email de {} envoyé : to={}", emailKind, message.getTo() != null ? message.getTo()[0] : "?");
        } catch (MailException e) {
            log.error("Échec de l'envoi de l'email de {} à {}", emailKind,
                    message.getTo() != null ? message.getTo()[0] : "?", e);
        }
    }
}