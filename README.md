# juribook-notification-service

Microservice de notifications pour **JuriBook** : consomme les événements Kafka publiés par `booking-service` (réservations, libération de créneaux) et envoie des emails réels aux clients et aux avocats, nouvelle demande, confirmation, rappel 24h, créneau libéré pour la liste d'attente.

## Stack

- Java 21 · Spring Boot 4.1.0 · Maven
- Spring Kafka (consumer : ce service ne produit aucun événement)
- Spring Mail (`JavaMailSender`) : envoi réel via SMTP (MailHog en local)
- Spring Security · JWT (validation, préparé pour un futur endpoint protégé : aucune route ne l'utilise encore)
- Spring Web (`RestClient`) : appels inter-services vers `booking-service`, `lawyer-service`, `auth-service`
- PostgreSQL 16 (base provisionnée, pas encore de persistance métier utilisée)
- Springdoc OpenAPI (Swagger UI)
- Port : **8084**

## Structure du projet

```
src/main/java/juribook/notification_service/
├── config/
│   ├── SecurityConfig.java               # permitAll partout, aucun endpoint métier protégé
│   ├── OpenApiConfig.java                # Configuration Swagger UI
│   └── JacksonConfig.java                # Bean ObjectMapper explicite (fix Spring Boot 4 / Jackson 3, cf. notes)
├── client/
│   ├── WaitlistEntryDto.java              # Miroir de WaitlistEntryResponse (booking-service)
│   ├── BookingDetailsDto.java             # Miroir de BookingHistoryResponse (booking-service)
│   ├── LawyerProfileDto.java              # Miroir PARTIEL de LawyerProfileResponse (id, authUserId, name)
│   ├── UserContactDto.java                # Miroir de UserContactResponse (auth-service)
│   ├── BookingServiceClient.java          # Interface - getWaitlist, getBookingDetails
│   ├── RestClientBookingServiceClient.java
│   ├── LawyerServiceClient.java           # Interface - getLawyer
│   ├── RestClientLawyerServiceClient.java
│   ├── AuthServiceClient.java             # Interface - getContact
│   └── RestClientAuthServiceClient.java
├── event/
│   ├── SlotReleasedEvent.java              # Miroir DTO du topic slot-events
│   ├── SlotReleasedEventConsumer.java      # @KafkaListener sur slot-events
│   ├── BookingEvent.java                   # Miroir DTO du topic booking-events
│   └── BookingEventConsumer.java           # @KafkaListener sur booking-events
├── notification/
│   ├── NotificationSender.java             # Interface — 4 méthodes d'envoi
│   └── EmailNotificationSender.java        # Impl unique — envoi réel via JavaMailSender
└── service/
    ├── SlotReleaseNotificationService.java      # Orchestration slot.released → liste d'attente
    ├── BookingConfirmationNotificationService.java  # Orchestration booking.confirmed → client
    ├── BookingRequestNotificationService.java       # Orchestration booking.created → avocat
    └── BookingReminderNotificationService.java      # Orchestration booking.reminder → client
src/main/resources/
└── application.yaml
```

## Lancer en local (hors Docker)

```bash
# Prérequis : booking-service, lawyer-service, auth-service accessibles
# (localhost:8083/8082/8081 par défaut), Kafka actif, un serveur SMTP
# (MailHog en local, cf. ci-dessous)
mvn spring-boot:run
```

Contrairement à `booking-service`, **Kafka n'est jamais désactivé** dans ce service, c'est sa raison d'être. Sans broker disponible, l'application démarre quand même (le consumer retente la connexion en arrière-plan), mais rien n'est consommé tant que Kafka n'est pas up.

## Lancer via Docker Compose

```bash
# Depuis juribook-docker/docker/
docker compose up -d
```

## Envoi d'emails — MailHog en local

Ce service envoie de **vrais emails** via `JavaMailSender`, pas des stubs qui se contentent de logger. En local, `spring.mail.host`/`port` pointent par défaut sur `localhost:1025`, le port SMTP standard de [MailHog](https://github.com/mailhog/MailHog), lancé via le service `mailhog` du `docker-compose.yml` racine. UI web pour consulter les emails reçus : [http://localhost:8025](http://localhost:8025).

Si aucun serveur SMTP n'écoute sur ce port, l'envoi échoue **proprement** : l'exception est catchée et logguée dans `EmailNotificationSender`, elle ne fait jamais planter le consumer Kafka qui a déclenché l'envoi. Toute la chaîne de résolution inter-services (booking/lawyer/auth) peut donc être vérifiée dans les logs même sans MailHog démarré.

## Swagger UI

[http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)

## Health check

[http://localhost:8084/actuator/health](http://localhost:8084/actuator/health)

---

## Ce que consomme ce service

Aucun endpoint REST métier, ce service est purement piloté par Kafka. Les deux `@KafkaListener` actifs :

| Topic | Consumer | Événements traités |
|---|---|---|
| `slot-events` | `SlotReleasedEventConsumer` | `slot.released` |
| `booking-events` | `BookingEventConsumer` | `booking.created`, `booking.confirmed`, `booking.cancelled`, `booking.reminder` |

Les deux consumers partagent le même `group-id` (`notification-service-group`, cf. `application.yaml`), sur des topics différents, pas de conflit de partitionnement entre eux.

### `slot.released` → liste d'attente

`SlotReleasedEventConsumer` reçoit `{lawyerId, slotId}`, rappelle `GET /api/waitlist/{lawyerId}` sur `booking-service` via `BookingServiceClient.getWaitlist`, puis notifie chaque client en attente (`NotificationSender.sendSlotReleasedNotification`), **reste un stub qui logue**, pas encore d'envoi réel pour ce cas précis (contrairement aux emails `booking.*`, cf. ci-dessous).

### `booking-events` → routage par type

`BookingEventConsumer` désérialise chaque message, logue systématiquement l'événement complet, puis route par `eventType` :

| `eventType` | Handler | Action | Sprint |
|---|---|---|---|
| `booking.created` | `handleBookingCreated` | Email à l'**avocat** - nouvelle demande PENDING | 5.4 |
| `booking.confirmed` | `handleBookingConfirmed` | Email au **client** - réservation confirmée | 5.3 |
| `booking.cancelled` | `handleBookingCancelled` | Routage + log uniquement | à venir |
| `booking.reminder` | `handleBookingReminder` | Email au **client** - rappel 24h | 5.5 |

Un message illisible (JSON malformé) est loggué en erreur et ignoré, sans faire planter le listener, même principe défensif que `SlotReleasedEventConsumer`.

---

## Résolution inter-services

Aucun des trois emails "riches" (confirmation, nouvelle demande, rappel) n'est composable à partir du seul contenu de l'événement Kafka — `BookingEvent` ne transporte que des ids (`bookingId`, `clientId`, `lawyerId`, `timeSlotId`), jamais de données lisibles (date/heure résolues, noms, emails). Chaque orchestrateur (`service/`) enchaîne les appels HTTP nécessaires, chacun défensif indépendamment (une erreur HTTP retourne `Optional.empty()`/liste vide plutôt que de lever une exception) :

### `BookingConfirmationNotificationService` (client, confirmation)
1. `booking-service` : `GET /api/bookings/{id}` → date/heure du créneau
2. `lawyer-service` : `GET /api/lawyers/{lawyerId}` → nom de l'avocat
3. `auth-service` : `GET /api/users/{clientId}/contact` → nom + email du client

### `BookingRequestNotificationService` (avocat, nouvelle demande) — un saut de plus
1. `booking-service` : `GET /api/bookings/{id}` → date/heure/motif
2. `lawyer-service` : `GET /api/lawyers/{lawyerId}` → nom **et `authUserId`** de l'avocat
3. `auth-service` : `GET /api/users/{authUserId}/contact` → email de l'avocat (résolu via son `authUserId`, pas son `lawyerId` — `lawyer-service` ne connaît jamais l'email, propriété exclusive de l'`auth-service`)
4. `auth-service` : `GET /api/users/{clientId}/contact` → nom du client (pour personnaliser le message)

### `BookingReminderNotificationService` (client, rappel 24h)
Structurellement identique à `BookingConfirmationNotificationService` — même besoin de données, même triple résolution. Séparé en classe dédiée plutôt que fusionné, pour rester cohérent avec le découpage "un type d'événement métier = un orchestrateur".

Si un maillon échoue (service down, id introuvable), l'orchestrateur logue un `WARN` explicite et **n'envoie rien**, le traitement de l'événement Kafka ne plante jamais pour un problème d'appel externe.

---

## Format des emails envoyés

Tous générés par `EmailNotificationSender`, texte brut (`SimpleMailMessage`), expéditeur fixe `no-reply@juribook.fr`. Dates formatées en français (`EEEE d MMMM yyyy`, ex: *lundi 6 juillet 2026*), heures au format `HH:mm`.

| Email | Sujet | Destinataire |
|---|---|---|
| Confirmation | `Votre rendez-vous avec {avocat} est confirmé` | Client |
| Nouvelle demande | `Nouvelle demande de rendez-vous de {client}` | Avocat |
| Rappel 24h | `Rappel : votre rendez-vous avec {avocat} demain` | Client |

---

## Variables d'environnement

| Variable | Description | Valeur par défaut |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL PostgreSQL | `jdbc:postgresql://localhost:5435/notificationdb` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Adresse Kafka | `localhost:9092` |
| `MAIL_HOST` / `MAIL_PORT` | Serveur SMTP | `localhost` / `1025` (MailHog) |
| `BOOKING_SERVICE_BASE_URL` | URL du booking-service | `http://localhost:8083` |
| `LAWYER_SERVICE_BASE_URL` | URL du lawyer-service | `http://localhost:8082` |
| `AUTH_SERVICE_BASE_URL` | URL de l'auth-service | `http://localhost:8081` |
| `JWT_SECRET` | Secret JWT partagé (validation uniquement) | valeur de dev |

---

## Notes techniques

### `ObjectMapper` (Jackson 2) non autoconfiguré par défaut

Spring Boot 4 utilise Jackson 3 (`JsonMapper`) en interne pour la sérialisation HTTP des `@RestController`, mais ce service n'a **aucun** `@RestController` métier (purement piloté par Kafka), donc même ce mécanisme ne se déclenche jamais. Sans intervention, aucun bean `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2 classique) n'existe dans le contexte, or les consumers Kafka (`SlotReleasedEventConsumer`, `BookingEventConsumer`) en dépendent explicitement pour désérialiser les payloads. `JacksonConfig.java` fournit ce bean explicitement (`new ObjectMapper().findAndRegisterModules()`).

### Piège d'ordre `@ConditionalOnBean`, évité par construction

`booking-service` a rencontré un bug où `@ConditionalOnBean(KafkaTemplate.class)` sur un `@Component` classique échouait systématiquement à cause de l'ordre de scan vs autoconfiguration Spring Boot. Ce service n'a jamais eu ce problème : contrairement à `booking-service`, Kafka n'y est **jamais conditionnellement désactivé**, les `@KafkaListener` sont inconditionnels, pas de branche NoOp à sélectionner au démarrage.

### Pas de persistance métier utilisée pour l'instant

Une base PostgreSQL (`notificationdb`) est provisionnée et le service s'y connecte au démarrage, mais aucune table métier n'est encore utilisée — pas de journal des notifications envoyées, pas de statut de lecture. Chaque email est envoyé "à la volée" au moment du traitement de l'événement Kafka, sans trace persistée. À envisager si un historique consultable (ex: "mes notifications") devient un besoin.

---

## Limites connues

- **`slot.released` reste un stub** (`sendSlotReleasedNotification` logue seulement) — contrairement aux trois emails `booking.*`, jamais passé en envoi réel. À faire en suivant exactement le même pattern que `sendBookingConfirmedEmail`/`sendReminderEmail`.
- **`booking.cancelled` n'a pas encore de notification réelle**, reste au stade routage + log (`handleBookingCancelled`), contrairement aux trois autres types d'événement de `booking-events`.
- **Aucune vérification que les emails de `auth-service`/`lawyer-service`/`booking-service` sont bien accessibles depuis ce service** au-delà d'un `WARN` loggué en cas d'échec — pas de retry, pas de dead-letter queue. Un service externe temporairement indisponible fait simplement échouer silencieusement (du point de vue utilisateur) la notification correspondante.
- **`GET /api/bookings/{id}`, `GET /api/lawyers/{id}` et `GET /api/users/{id}/contact` sont tous publics, sans authentification applicative** entre services (pas d'API key, pas de réseau interne isolé), cf. les README respectifs de `booking-service` et `auth-service` pour le détail de cette limite partagée.