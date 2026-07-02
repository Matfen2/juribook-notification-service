package juribook.notification_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Miroir PARTIEL de LawyerProfileResponse (lawyer-service), seuls les
 * champs utilisés sont désérialisés (name, authUserId), pas tout le
 * profil. @JsonIgnoreProperties(ignoreUnknown = true) évite un échec de
 * désérialisation sur les nombreux champs ignorés ici (bio, tarif,
 * spécialités, adresse...).
 *
 * authUserId : nécessaire pour résoudre l'email de
 * l'avocat via auth-service, lawyer-service ne connaît que le nom
 * (dénormalisé à la création du profil), jamais l'email, qui reste
 * la propriété exclusive de l'auth-service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LawyerProfileDto(Long id, Long authUserId, String name) {
}