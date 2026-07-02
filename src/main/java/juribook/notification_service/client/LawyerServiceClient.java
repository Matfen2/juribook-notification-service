package juribook.notification_service.client;

import java.util.Optional;

/**
 * Abstraction de l'appel inter-services vers le lawyer-service, pour
 * résoudre le nom d'un avocat à partir de son id (utilisé dans l'email
 * de confirmation au client).
 */
public interface LawyerServiceClient {

    Optional<LawyerProfileDto> getLawyer(Long lawyerId);
}