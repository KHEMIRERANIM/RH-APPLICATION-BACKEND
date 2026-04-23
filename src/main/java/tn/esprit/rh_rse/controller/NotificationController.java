package tn.esprit.rh_rse.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.config.jwt.JwtUtil;
import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    private String extraireIdUser(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            String jwt = headerAuth.substring(7);
            return jwtUtil.extractUserId(jwt);
        }
        return null;
    }

    // --- Combined Endpoints ---

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        if (idUser != null) {
            // Priority to user's notifications (Mutuelle use case)
            return ResponseEntity.ok(notificationService.getMesNotifications(idUser));
        }
        // Fallback to all (Transport use case / Admin)
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getById(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id, HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        if (idUser != null) {
            try {
                notificationService.supprimerNotification(id, idUser);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                // If not authorized or other error, fallback to simple delete if possible or rethrow
            }
        }
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/lire")
    public ResponseEntity<Notification> marquerCommeLuPatch(@PathVariable String id, HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        if (idUser != null) {
            return ResponseEntity.ok(notificationService.marquerCommeLu(id, idUser));
        }
        return ResponseEntity.ok(notificationService.marquerCommeLu(id));
    }

    @PutMapping("/{id}/lire")
    public ResponseEntity<Notification> marquerCommeLuPut(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.marquerCommeLu(id));
    }

    @PatchMapping("/tout-lire")
    public ResponseEntity<Void> marquerToutCommeLu(HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        if (idUser != null) {
            notificationService.marquerToutCommeLu(idUser);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getCount(HttpServletRequest request) {
        String idUser = extraireIdUser(request);
        if (idUser != null) {
            return ResponseEntity.ok(notificationService.getNbNonLues(idUser));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // --- Transport Specific Endpoints ---

    @GetMapping("/destinataire/{destinataireId}")
    public ResponseEntity<List<Notification>> getByDestinataire(@PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.getByDestinataire(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/non-lues")
    public ResponseEntity<List<Notification>> getNonLues(@PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.getNonLues(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/count")
    public ResponseEntity<Long> countNonLues(@PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.countNonLues(destinataireId));
    }

    @PutMapping("/destinataire/{destinataireId}/lire-tout")
    public ResponseEntity<Void> marquerToutesCommeLues(@PathVariable String destinataireId) {
        notificationService.marquerToutesCommeLues(destinataireId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/demande-confirmation")
    public ResponseEntity<Notification> demandeConfirmation(
            @RequestParam String conducteurId,
            @RequestParam String passagerId,
            @RequestParam String trajetId,
            @RequestParam String reservationId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.envoyerDemandeConfirmation(
                        conducteurId, passagerId, trajetId, reservationId));
    }

    @PostMapping
    public ResponseEntity<Notification> create(@RequestBody Notification notification) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.create(notification));
    }
}
