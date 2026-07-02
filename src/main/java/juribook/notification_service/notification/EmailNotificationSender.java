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
 * sendBookingConfirmedEmail envoie un VRAI email via
 * JavaMailSender, autoconfiguré par Spring Boot dès que spring.mail.host
 * est renseigné (cf. application.yaml - localhost:1025 par défaut,
 * port standard MailHog/Mailpit pour tester en local sans vrai
 * provider SMTP). Si aucun serveur SMTP n'écoute sur ce port, l'envoi
 * échoue proprement : l'exception est catchée et logguée, elle ne fait
 * jamais planter le consumer Kafka qui a déclenché l'envoi.
 *
 * sendSlotReleasedNotification reste un stub qui logue
 * pas dans le scope de ce sprint, à faire passer en envoi réel plus tard
 * en suivant exactement le même pattern.
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

        try {
            mailSender.send(message);
            log.info("Email de confirmation envoyé : to={}, rendez-vous le {} à {}",
                    toEmail, date, startTime);
        } catch (MailException e) {
            // Un échec d'envoi (pas de serveur SMTP disponible, adresse
            // invalide, etc.) est loggué mais ne remonte jamais jusqu'au
            // consumer Kafka appelant, la réservation reste confirmée
            // côté métier même si l'email n'est pas parti.
            log.error("Échec de l'envoi de l'email de confirmation à {}", toEmail, e);
        }
    }
}