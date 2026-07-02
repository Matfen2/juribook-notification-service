package juribook.notification_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Notification in-app persistée, consultable par le frontend via
 * polling.
 *
 * recipientAuthUserId est toujours l'authUserId (auth-service), jamais
 * un clientId/lawyerId d'un autre service, c'est ce qui arrive dans le
 * claim "id" du JWT côté frontend, donc ce qui permet de résoudre
 * "mes notifications" à partir de l'utilisateur connecté. Pour un
 * client, clientId == authUserId directement (pas de conversion). Pour
 * un avocat, il faut résoudre lawyerId → authUserId via le lawyer-service
 * avant de créer la notification (cf. BookingRequestNotificationService,
 * même logique déjà utilisée pour résoudre l'email de l'avocat).
 */
@Entity
@Table(name = "notifications")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_auth_user_id", nullable = false)
    private Long recipientAuthUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    // Message déjà formé en français, prêt à afficher tel quel côté
    // frontend — pas de template à recomposer côté client.
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    // Référence optionnelle vers la réservation concernée, pour un futur
    // lien direct "Voir la réservation" côté frontend.
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}