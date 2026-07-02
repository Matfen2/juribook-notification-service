package juribook.notification_service.client;

/**
 * Miroir de UserContactResponse (auth-service). Désérialise la réponse
 * de GET /api/users/{id}/contact : nom + email minimal, pas le profil
 * complet.
 */
public record UserContactDto(Long id, String name, String email) {
}