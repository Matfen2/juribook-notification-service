package juribook.notification_service.entity;

/**
 * Type d'une notification in-app. Correspond 1:1 aux événements
 * booking-events/slot-events déjà déclencheurs d'un email
 * , chaque email envoyé produit aussi une notification persistée, pour que
 * le frontend puisse l'afficher sans recharger (Sprint 5.6).
 */
public enum NotificationType {
    BOOKING_CREATED,    // → avocat, nouvelle demande PENDING
    BOOKING_CONFIRMED,  // → client, réservation confirmée
    BOOKING_REMINDER,   // → client, rappel 24h
    SLOT_RELEASED       // → client en liste d'attente, créneau libéré
}