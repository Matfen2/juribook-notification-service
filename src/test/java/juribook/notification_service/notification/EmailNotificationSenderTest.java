package juribook.notification_service.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests des templates email envoyés par EmailNotificationSender
 * , vérifie le sujet et le contenu du corps pour chacun
 * des 4 types, plus le comportement défensif sur échec d'envoi (jamais
 * d'exception propagée).
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationSender sender;

    private static final LocalDate DATE = LocalDate.of(2026, 7, 6);
    private static final LocalTime START = LocalTime.of(9, 0);
    private static final LocalTime END = LocalTime.of(9, 30);

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        sender = new EmailNotificationSender(mailSender);
    }

    // ══════════════════════════════════════════════════════════
    //  Confirmation
    // ══════════════════════════════════════════════════════════
    @Test
    void sendBookingConfirmedEmail_hasCorrectSubjectAndRecipient() {
        sender.sendBookingConfirmedEmail("jean@test.com", "Jean Dupont", "Sophie Martin", DATE, START, END);

        SimpleMailMessage sent = captureSentMessage();
        assertThat(sent.getTo()).containsExactly("jean@test.com");
        assertThat(sent.getFrom()).isEqualTo("no-reply@juribook.fr");
        assertThat(sent.getSubject()).isEqualTo("Votre rendez-vous avec Sophie Martin est confirmé");
    }

    @Test
    void sendBookingConfirmedEmail_bodyContainsClientNameDateAndTime() {
        sender.sendBookingConfirmedEmail("jean@test.com", "Jean Dupont", "Sophie Martin", DATE, START, END);

        String body = captureSentMessage().getText();
        assertThat(body)
                .contains("Jean Dupont")
                .contains("Sophie Martin")
                .contains("lundi 6 juillet 2026")
                .contains("09:00")
                .contains("09:30");
    }

    // ══════════════════════════════════════════════════════════
    //  Nouvelle demande
    // ══════════════════════════════════════════════════════════
    @Test
    void sendNewBookingRequestEmail_hasCorrectSubjectAndRecipient() {
        sender.sendNewBookingRequestEmail(
                "sophie@test.com", "Sophie Martin", "Jean Dupont", DATE, START, END, "Litige avec mon employeur");

        SimpleMailMessage sent = captureSentMessage();
        assertThat(sent.getTo()).containsExactly("sophie@test.com");
        assertThat(sent.getSubject()).isEqualTo("Nouvelle demande de rendez-vous de Jean Dupont");
    }

    @Test
    void sendNewBookingRequestEmail_bodyContainsReason() {
        sender.sendNewBookingRequestEmail(
                "sophie@test.com", "Sophie Martin", "Jean Dupont", DATE, START, END, "Litige avec mon employeur");

        assertThat(captureSentMessage().getText()).contains("Litige avec mon employeur");
    }

    @Test
    void sendNewBookingRequestEmail_blankReason_fallsBackToDefaultText() {
        sender.sendNewBookingRequestEmail(
                "sophie@test.com", "Sophie Martin", "Jean Dupont", DATE, START, END, "   ");

        assertThat(captureSentMessage().getText()).contains("Non renseigné");
    }

    @Test
    void sendNewBookingRequestEmail_nullReason_fallsBackToDefaultText() {
        sender.sendNewBookingRequestEmail(
                "sophie@test.com", "Sophie Martin", "Jean Dupont", DATE, START, END, null);

        assertThat(captureSentMessage().getText()).contains("Non renseigné");
    }

    // ══════════════════════════════════════════════════════════
    //  Rappel 24h
    // ══════════════════════════════════════════════════════════
    @Test
    void sendReminderEmail_hasCorrectSubject() {
        sender.sendReminderEmail("jean@test.com", "Jean Dupont", "Sophie Martin", DATE, START, END);

        assertThat(captureSentMessage().getSubject())
                .isEqualTo("Rappel : votre rendez-vous avec Sophie Martin demain");
    }

    @Test
    void sendReminderEmail_bodyMentionsTomorrow() {
        sender.sendReminderEmail("jean@test.com", "Jean Dupont", "Sophie Martin", DATE, START, END);

        assertThat(captureSentMessage().getText()).contains("demain");
    }

    // ══════════════════════════════════════════════════════════
    //  Annulation
    // ══════════════════════════════════════════════════════════
    @Test
    void sendCancellationEmail_withSchedule_includesDateAndTime() {
        sender.sendCancellationEmail("jean@test.com", "Jean Dupont", "Sophie Martin", DATE, START, END);

        String body = captureSentMessage().getText();
        assertThat(body).contains("lundi 6 juillet 2026").contains("09:00").contains("09:30");
    }

    @Test
    void sendCancellationEmail_withoutSchedule_doesNotThrow_omitsScheduleLine() {
        // Créneau introuvable (date/startTime/endTime null), ne doit
        // jamais lever de NullPointerException sur un format(null).
        assertDoesNotThrow(() ->
                sender.sendCancellationEmail("jean@test.com", "Jean Dupont", "Sophie Martin", null, null, null));

        String body = captureSentMessage().getText();
        assertThat(body).contains("Jean Dupont").contains("Sophie Martin");
        assertThat(body).doesNotContain("null");
    }

    @Test
    void sendCancellationEmail_hasCorrectSubject() {
        sender.sendCancellationEmail("jean@test.com", "Jean Dupont", "Sophie Martin", DATE, START, END);

        assertThat(captureSentMessage().getSubject())
                .isEqualTo("Votre rendez-vous avec Sophie Martin a été annulé");
    }

    // ══════════════════════════════════════════════════════════
    //  Comportement défensif sur échec d'envoi
    // ══════════════════════════════════════════════════════════
    @Test
    void send_mailServerUnavailable_doesNotPropagateException() {
        doThrow(new org.springframework.mail.MailSendException("SMTP indisponible"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Aucune des 4 méthodes ne doit jamais laisser fuiter une
        // MailException vers l'appelant (le consumer Kafka), sans ça,
        // un serveur SMTP down ferait planter le traitement de
        // l'événement, pas juste l'envoi de l'email.
        assertDoesNotThrow(() ->
                sender.sendBookingConfirmedEmail("jean@test.com", "Jean Dupont", "Sophie Martin", DATE, START, END));
    }

    // ══════════════════════════════════════════════════════════
    //  Helper
    // ══════════════════════════════════════════════════════════
    private SimpleMailMessage captureSentMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        return captor.getValue();
    }
}