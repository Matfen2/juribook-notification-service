# juribook-notification-service

Microservice de notifications pour **JuriBook** : consomme les événements Kafka publiés par `booking-service` (réservations, libération de créneaux) et envoie des emails réels **et** des notifications in-app aux clients et aux avocats, nouvelle demande, confirmation, rappel 24h, annulation, créneau libéré pour la liste d'attente.

## Stack

- Java 21 · Spring Boot 4.1.0 · Maven
- Spring Kafka (consumer — ce service ne produit aucun événement)
- Spring Mail (`JavaMailSender`) - envoi réel via SMTP (MailHog en local)
- Spring Data JPA · PostgreSQL 16 · Flyway
- Spring Security · JWT (validation - `/api/notifications/**` exige un token valide, n'importe quel rôle)
- Spring Web (`RestClient`) - appels inter-services vers `booking-service`, `lawyer-service`, `auth-service`
- Springdoc OpenAPI (Swagger UI)
- Port : **8084**

## Structure du projet

```
src/main/java/juribook/notification_service/
├── config/
│   ├── SecurityConfig.java               # /api/notifications/** authentifié, reste public (actuator/swagger)
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
├── controller/
│   └── NotificationController.java        # GET /api/notifications, /unread-count, PATCH /{id}/read
├── dto/response/
│   └── NotificationResponse.java
├── entity/
│   ├── Notification.java                  # Notification in-app persistée
│   └── NotificationType.java              # BOOKING_CREATED | CONFIRMED | REMINDER | CANCELLED | SLOT_RELEASED
├── event/
│   ├── SlotReleasedEvent.java              # Miroir DTO du topic slot-events
│   ├── SlotReleasedEventConsumer.java      # @KafkaListener sur slot-events
│   ├── BookingEvent.java                   # Miroir DTO du topic booking-events
│   └── BookingEventConsumer.java           # @KafkaListener sur booking-events
├── exception/
│   ├── GlobalExceptionHandler.java         # 404/403
│   └── NotificationNotFoundException.java
├── notification/
│   ├── NotificationSender.java             # Interface - 5 méthodes d'envoi
│   └── EmailNotificationSender.java        # Impl unique - envoi réel via JavaMailSender
├── repository/
│   └── NotificationRepository.java
└── service/
    ├── SlotReleaseNotificationService.java          # Orchestration slot.released → liste d'attente
    ├── BookingConfirmationNotificationService.java  # Orchestration booking.confirmed → client
    ├── BookingRequestNotificationService.java       # Orchestration booking.created → avocat
    ├── BookingReminderNotificationService.java      # Orchestration booking.reminder → client
    ├── BookingCancellationNotificationService.java  # Orchestration booking.cancelled → client 
    └── NotificationService.java                     # Persistance des notifications in-app 
src/main/resources/
├── application.yaml
└── db/migration/
    └── V1__create_notifications_table.sql   # Première migration de ce service 
src/test/java/juribook/notification_service/   
├── event/
│   ├── BookingEventConsumerTest.java
│   └── SlotReleasedEventConsumerTest.java
├── notification/
│   └── EmailNotificationSenderTest.java
└── service/
    ├── BookingConfirmationNotificationServiceTest.java
    ├── BookingRequestNotificationServiceTest.java
    ├── BookingReminderNotificationServiceTest.java
    ├── BookingCancellationNotificationServiceTest.java
    └── NotificationServiceTest.java
```

## Lancer en local (hors Docker)

```bash
# Prérequis : PostgreSQL sur localhost:5435 avec la base notificationdb,
# booking-service/lawyer-service/auth-service accessibles
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

## Lancer les tests

```bash
mvn test
```

## Envoi d'emails — MailHog en local

Ce service envoie de **vrais emails** via `JavaMailSender`, pas des stubs qui se contentent de logger (sauf `slot.released`, cf. [Limites connues](#limites-connues)). En local, `spring.mail.host`/`port` pointent par défaut sur `localhost:1025`, le port SMTP standard de [MailHog](https://github.com/mailhog/MailHog), lancé via le service `mailhog` du `docker-compose.yml` racine. UI web pour consulter les emails reçus : [http://localhost:8025](http://localhost:8025).

Si aucun serveur SMTP n'écoute sur ce port, l'envoi échoue **proprement** : l'exception est catchée et logguée dans `EmailNotificationSender`, elle ne fait jamais planter le consumer Kafka qui a déclenché l'envoi.

## Swagger UI

[http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html)

## Health check

[http://localhost:8084/actuator/health](http://localhost:8084/actuator/health)

---

## Notifications in-app

Chaque email "riche" (confirmation, nouvelle demande, rappel, annulation) déclenche **aussi** une notification persistée en base, consultable par le frontend via polling, même déclencheur, deux canaux.

### Endpoints (JWT requis, n'importe quel rôle - CLIENT ou LAWYER)

| Méthode | URL | Description |
|---|---|---|
| `GET` | `/api/notifications` | Toutes mes notifications, plus récente en premier |
| `GET` | `/api/notifications/unread-count` | Compteur léger, dédié au polling du badge |
| `PATCH` | `/api/notifications/{id}/read` | Marquer comme lue - 403 si elle n'appartient pas à l'appelant |

`recipientAuthUserId` est toujours l'`authUserId` (auth-service), jamais un `clientId`/`lawyerId` d'un autre service, c'est ce qui arrive dans le claim `id` du JWT côté frontend. Pour un client, `clientId == authUserId` directement. Pour un avocat, il faut résoudre `lawyerId → authUserId` via le `lawyer-service` avant de créer la notification (même logique que pour résoudre son email, cf. `BookingRequestNotificationService`).

### Exemple

```
GET http://localhost:8084/api/notifications
Authorization: Bearer <token>
```
```json
[
    {
        "id": 2,
        "type": "BOOKING_CONFIRMED",
        "message": "Votre rendez-vous avec Sophie Martin est confirmé pour le 6 juillet 2026 à 11:30",
        "bookingId": 9,
        "read": false,
        "createdAt": "2026-07-02T21:42:52.846526"
    }
]
```

---

## Ce que consomme ce service

Aucun endpoint REST métier hors notifications in-app, le cœur du service reste piloté par Kafka. Les deux `@KafkaListener` actifs :

| Topic | Consumer | Événements traités |
|---|---|---|
| `slot-events` | `SlotReleasedEventConsumer` | `slot.released` |
| `booking-events` | `BookingEventConsumer` | `booking.created`, `booking.confirmed`, `booking.cancelled`, `booking.reminder` |

Les deux consumers partagent le même `group-id` (`notification-service-group`), sur des topics différents, pas de conflit de partitionnement entre eux.

### `slot.released` → liste d'attente

`SlotReleasedEventConsumer` reçoit `{lawyerId, slotId}`, rappelle `GET /api/waitlist/{lawyerId}` sur `booking-service` via `BookingServiceClient.getWaitlist`, puis notifie chaque client en attente (`NotificationSender.sendSlotReleasedNotification`), **reste un stub qui logue**, pas encore d'envoi réel ni de notification in-app pour ce cas précis.

### `booking-events` → routage par type

`BookingEventConsumer` désérialise chaque message, logue systématiquement l'événement complet, puis route par `eventType` :

| `eventType` | Handler | Action | Sprint |
|---|---|---|---|
| `booking.created` | `handleBookingCreated` | Email + notification in-app à l'**avocat** - nouvelle demande PENDING
| `booking.confirmed` | `handleBookingConfirmed` | Email + notification in-app au **client** - réservation confirmée
| `booking.cancelled` | `handleBookingCancelled` | Email + notification in-app au **client** - annulation (refus, annulation manuelle, ou désactivation d'avocat)
| `booking.reminder` | `handleBookingReminder` | Email + notification in-app au **client** - rappel 24h

Un message illisible (JSON malformé) est loggué en erreur et ignoré, sans faire planter le listener, même principe défensif que `SlotReleasedEventConsumer`.

---

## Résolution inter-services

Aucun des emails "riches" n'est composable à partir du seul contenu de l'événement Kafka, `BookingEvent` ne transporte que des ids (`bookingId`, `clientId`, `lawyerId`, `timeSlotId`), jamais de données lisibles. Chaque orchestrateur (`service/`) enchaîne les appels HTTP nécessaires, chacun défensif indépendamment (une erreur HTTP retourne `Optional.empty()`/liste vide plutôt que de lever une exception) :

### `BookingConfirmationNotificationService` (client, confirmation)
1. `booking-service` : `GET /api/bookings/{id}` → date/heure du créneau
2. `lawyer-service` : `GET /api/lawyers/{lawyerId}` → nom de l'avocat
3. `auth-service` : `GET /api/users/{clientId}/contact` → nom + email du client

### `BookingRequestNotificationService` (avocat, nouvelle demande) — un saut de plus
1. `booking-service` : `GET /api/bookings/{id}` → date/heure/motif
2. `lawyer-service` : `GET /api/lawyers/{lawyerId}` → nom **et `authUserId`** de l'avocat
3. `auth-service` : `GET /api/users/{authUserId}/contact` → email de l'avocat (résolu via son `authUserId`, pas son `lawyerId`)
4. `auth-service` : `GET /api/users/{clientId}/contact` → nom du client (pour personnaliser le message)

### `BookingReminderNotificationService` (client, rappel 24h)
Structurellement identique à `BookingConfirmationNotificationService`.

### `BookingCancellationNotificationService` (client, annulation)
Couvre les trois origines possibles d'un `booking.cancelled` (refus par l'avocat, annulation manuelle, désactivation de l'avocat pour une demande `PENDING`) sans distinction : le client reçoit le même email dans les trois cas.

⚠️ **Seul orchestrateur qui tolère un détail de créneau manquant** : au moment où ce handler tourne, le `TimeSlot` a potentiellement déjà été remis à `AVAILABLE`, voire réservé par quelqu'un d'autre entre-temps. `getBookingDetails` peut donc échouer sans bloquer l'envoi, l'email part quand même, sans la ligne date/heure (`EmailNotificationSender.sendCancellationEmail` gère ce cas explicitement, aucun `NullPointerException` sur un `.format(null)`).

Si un maillon échoue (service down, id introuvable), les 3 autres orchestrateurs loguent un `WARN` explicite et **n'envoient rien**, le traitement de l'événement Kafka ne plante jamais pour un problème d'appel externe.

---

## Format des emails envoyés

Tous générés par `EmailNotificationSender`, texte brut (`SimpleMailMessage`), expéditeur fixe `no-reply@juribook.fr`. Dates formatées en français (`EEEE d MMMM yyyy`, ex: *lundi 6 juillet 2026*), heures au format `HH:mm`.

| Email | Sujet | Destinataire |
|---|---|---|
| Confirmation | `Votre rendez-vous avec {avocat} est confirmé` | Client |
| Nouvelle demande | `Nouvelle demande de rendez-vous de {client}` | Avocat |
| Rappel 24h | `Rappel : votre rendez-vous avec {avocat} demain` | Client |
| Annulation | `Votre rendez-vous avec {avocat} a été annulé` | Client |

---

## Tests (Sprint 5.11)

```bash
mvn test
```

| Fichier | Ce qu'il couvre |
|---|---|
| `event/BookingEventConsumerTest.java` | Routage des 4 `eventType` vers le bon orchestrateur, JSON malformé toléré |
| `event/SlotReleasedEventConsumerTest.java` | Idem pour `slot.released` |
| `notification/EmailNotificationSenderTest.java` | Sujet + corps des 4 templates, panne SMTP jamais propagée |
| `service/BookingConfirmationNotificationServiceTest.java` | Cas nominal + chaque branche d'échec (détails/avocat/client manquant) |
| `service/BookingRequestNotificationServiceTest.java` | Idem + vérifie spécifiquement la résolution `lawyerId → authUserId` |
| `service/BookingReminderNotificationServiceTest.java` | Structurellement identique au test de confirmation |
| `service/BookingCancellationNotificationServiceTest.java` | Seul test qui vérifie la tolérance à un créneau manquant |
| `service/NotificationServiceTest.java` | Persistance + contrôle d'appartenance (`AccessDeniedException` si notification d'un autre utilisateur) |

Kafka lui-même n'est jamais impliqué dans les tests des consumers, `onBookingEvent(String)`/`onSlotEvent(String)` sont appelés directement avec un payload JSON brut, exactement comme le ferait le conteneur Spring Kafka après désérialisation.

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

Spring Boot 4 utilise Jackson 3 (`JsonMapper`) en interne pour la sérialisation HTTP des `@RestController`, ce service a désormais `NotificationController`, mais ce mécanisme ne crée toujours pas de bean `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2 classique), or les consumers Kafka en dépendent explicitement pour désérialiser les payloads. `JacksonConfig.java` fournit ce bean explicitement.

### Piège d'ordre `@ConditionalOnBean`, jamais rencontré ici

`booking-service` a corrigé un bug où `@ConditionalOnBean(KafkaTemplate.class)` sur un `@Component` classique échouait systématiquement à cause de l'ordre de scan vs autoconfiguration. Ce service n'a jamais eu ce problème : Kafka n'y est **jamais conditionnellement désactivé**, les `@KafkaListener` sont inconditionnels.

### Persistance - première utilisation 

La base `notificationdb` était provisionnée dès le début mais totalement inutilisée (aucune entité, aucun repository). `V1__create_notifications_table.sql` est donc la toute première migration de ce service.

---

## Limites connues

- **`slot.released` reste un stub, ni email réel ni notification in-app** (`sendSlotReleasedNotification` logue seulement), contrairement aux quatre événements `booking.*`. À faire en suivant exactement le même pattern.
- **Aucune vérification que `auth-service`/`lawyer-service`/`booking-service` sont bien accessibles** au-delà d'un `WARN` loggué en cas d'échec, pas de retry, pas de dead-letter queue.
- **`GET /api/bookings/{id}`, `GET /api/lawyers/{id}` et `GET /api/users/{id}/contact` sont tous publics, sans authentification applicative** entre services — cf. les README de `booking-service` et `auth-service`.
- **Pas de test dédié pour `NotificationController`** (Sprint 5.6), seul `NotificationService` (la couche métier en dessous) est couvert par les tests du 5.11 ; le controller lui-même (mapping des query params, extraction du principal) n'a pas de test d'intégration.