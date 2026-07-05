package juribook.notification_service.entity;

public enum NotificationType {
    BOOKING_CREATED,    // → avocat, nouvelle demande PENDING
    BOOKING_CONFIRMED,  // → client, réservation confirmée
    BOOKING_REMINDER,   // → client, rappel 24h
    BOOKING_CANCELLED,  // → client, annulation
    SLOT_RELEASED,      // → client en liste d'attente, créneau libéré
    DOCUMENT_READY      // → client, document traité et prêt
}