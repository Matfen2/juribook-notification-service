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

/**
 * Envoi des notifications par email.
 *
 * sendBookingConfirmedEmail et sendNewBookingRequestEmail
 * envoient de VRAIS emails via JavaMailSender, autoconfiguré
 * par Spring Boot dès que spring.mail.host est renseigné (cf.
 * application.yaml, MailHog en local). Si aucun serveur SMTP n'écoute,
 * l'envoi échoue proprement : l'exception est catchée et logguée, elle
 * ne fait jamais planter le consumer Kafka qui a déclenché l'envoi.
 *
 * sendSlotReleasedNotification reste un stub qui logue —
 * à faire passer en envoi réel plus tard en suivant le même pattern.
 */
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
                clientName,
                lawyerName,
                date.format(DATE_FORMATTER),
                startTime.format(TIME_FORMATTER),
                endTime.format(TIME_FORMATTER)
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
                lawyerName,
                clientName,
                date.format(DATE_FORMATTER),
                startTime.format(TIME_FORMATTER),
                endTime.format(TIME_FORMATTER),
                reason != null && !reason.isBlank() ? reason : "Non renseigné"
        ));

        send(message, "nouvelle demande");
    }

    private void send(SimpleMailMessage message, String emailKind) {
        try {
            mailSender.send(message);
            log.info("Email de {} envoyé : to={}", emailKind, message.getTo() != null ? message.getTo()[0] : "?");
        } catch (MailException e) {
            // Un échec d'envoi (pas de serveur SMTP disponible, adresse
            // invalide, etc.) est loggué mais ne remonte jamais jusqu'au
            // consumer Kafka appelant.
            log.error("Échec de l'envoi de l'email de {} à {}", emailKind,
                    message.getTo() != null ? message.getTo()[0] : "?", e);
        }
    }
}