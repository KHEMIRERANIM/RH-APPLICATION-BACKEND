package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.NotificationTransport;
import tn.esprit.rh_rse.service.NotificationTransportService;

import java.util.List;

@RestController
@RequestMapping("/api/transport-notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class NotificationTransportController {

    private final NotificationTransportService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationTransport>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationTransport> getById(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.getById(id));
    }

    @GetMapping("/destinataire/{destinataireId}")
    public ResponseEntity<List<NotificationTransport>> getByDestinataire(@PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.getByDestinataire(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/non-lues")
    public ResponseEntity<List<NotificationTransport>> getNonLues(@PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.getNonLues(destinataireId));
    }

    @GetMapping("/destinataire/{destinataireId}/count")
    public ResponseEntity<Long> countNonLues(@PathVariable String destinataireId) {
        return ResponseEntity.ok(notificationService.countNonLues(destinataireId));
    }

    @PostMapping
    public ResponseEntity<NotificationTransport> create(@RequestBody NotificationTransport notification) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.create(notification));
    }

    @PutMapping("/{id}/lire")
    public ResponseEntity<NotificationTransport> marquerCommeLu(@PathVariable String id) {
        return ResponseEntity.ok(notificationService.marquerCommeLu(id));
    }

    @PutMapping("/destinataire/{destinataireId}/lire-tout")
    public ResponseEntity<Void> marquerToutesCommeLues(@PathVariable String destinataireId) {
        notificationService.marquerToutesCommeLues(destinataireId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
