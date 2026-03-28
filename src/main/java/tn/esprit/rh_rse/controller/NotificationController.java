package tn.esprit.rh_rse.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getById(
            @PathVariable String id) {
        return ResponseEntity.ok(notificationService.getById(id));
    }

    @GetMapping("/destinataire/{destinataireId}")
    public ResponseEntity<List<Notification>> getByDestinataire(
            @PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.getByDestinataire(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/non-lues")
    public ResponseEntity<List<Notification>> getNonLues(
            @PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.getNonLues(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/count")
    public ResponseEntity<Long> countNonLues(
            @PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.countNonLues(destinataireId));
    }

    @PutMapping("/{id}/lire")
    public ResponseEntity<Notification> marquerCommeLu(
            @PathVariable String id) {
        return ResponseEntity.ok(notificationService.marquerCommeLu(id));
    }

    @PutMapping("/destinataire/{destinataireId}/lire-tout")
    public ResponseEntity<Void> marquerToutesCommeLues(
            @PathVariable String destinataireId) {
        notificationService.marquerToutesCommeLues(destinataireId);
        return ResponseEntity.noContent().build();
    }

    // ─── DEMANDE CONFIRMATION ──────────────────────────────
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
    public ResponseEntity<Notification> create(
            @RequestBody Notification notification) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.create(notification));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}