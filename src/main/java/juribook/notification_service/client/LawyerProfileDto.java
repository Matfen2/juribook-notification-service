package juribook.notification_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Miroir PARTIEL de LawyerProfileResponse (lawyer-service), on ne
 * désérialise que le champ utilisé (name), pas tout le profil.
 * @JsonIgnoreProperties(ignoreUnknown = true) évite un échec de
 * désérialisation sur les nombreux champs du DTO réel qu'on ignore ici
 * (bio, tarif, spécialités, adresse...).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LawyerProfileDto(Long id, String name) {
}