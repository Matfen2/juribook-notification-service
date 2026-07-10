package juribook.notification_service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public EmailNotificationSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendSlotReleasedNotification(Long clientId, Long lawyerId, Long slotId) {
        log.info("[EMAIL STUB] Notification créneau libéré → clientId={}, lawyerId={}, slotId={}. "
                + "Un email serait envoyé ici en production.", clientId, lawyerId, slotId);
    }

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

    @Override
    public void sendAbuseAlertEmail(String adminEmail, Long actorId, String reason,
                                     long signalCount, LocalDateTime occurredAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(FROM_ADDRESS);
        message.setTo(adminEmail);
        message.setSubject("[JuriBook] Abus détecté - compte #" + actorId);
        message.setText("""
                Un compte a dépassé un seuil de détection d'abus et a été suspendu automatiquement.

                Compte concerné : #%d
                Motif : %s
                Nombre de signaux dans la fenêtre : %d
                Détecté le : %s

                Consultez le journal d'audit pour le détail complet des événements
                ayant mené à cette détection.
                """.formatted(actorId, reason, signalCount, occurredAt));

        send(message, "alerte abus admin");
    }

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