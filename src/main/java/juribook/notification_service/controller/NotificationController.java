package juribook.notification_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import juribook.notification_service.dto.response.NotificationResponse;
import juribook.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST des notifications in-app.
 *
 * Toutes les routes exigent un JWT valide (n'importe quel rôle : CLIENT
 * ou LAWYER), via anyRequest().authenticated() déjà en place dans
 * SecurityConfig, pas de règle spécifique à ajouter, ces routes ne
 * figurent pas dans la liste des URLs publiques.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications in-app")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Mes notifications", description = "Toutes mes notifications, triées de la plus récente à la plus ancienne")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication authentication) {
        Long authUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(notificationService.getMyNotifications(authUserId));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de notifications non lues", description = "Endpoint léger destiné au polling du badge, évite de refetcher la liste complète à chaque poll")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long authUserId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(authUserId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long authUserId = (Long) authentication.getPrincipal();
        notificationService.markAsRead(authUserId, id);
        return ResponseEntity.noContent().build();
    }
}