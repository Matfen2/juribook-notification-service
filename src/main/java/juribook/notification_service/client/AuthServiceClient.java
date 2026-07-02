package juribook.notification_service.client;

import java.util.Optional;

/**
 * Abstraction de l'appel inter-services vers l'auth-service, pour
 * résoudre nom + email d'un utilisateur à partir de son id.
 */
public interface AuthServiceClient {

    Optional<UserContactDto> getContact(Long userId);
}