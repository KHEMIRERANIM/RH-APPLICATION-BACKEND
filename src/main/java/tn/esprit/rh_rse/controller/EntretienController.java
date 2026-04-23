package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.CreateEntretienRequest;
import tn.esprit.rh_rse.dto.request.FeedbackEntretienRequest;
import tn.esprit.rh_rse.dto.response.EntretienResponse;
import tn.esprit.rh_rse.service.EntretienService;

import java.util.List;

@RestController
@RequestMapping("/api/recrutement/entretiens")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class EntretienController {

    private final EntretienService entretienService;

    @PostMapping
    public ResponseEntity<EntretienResponse> planifierEntretien(
            @RequestBody CreateEntretienRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entretienService.planifierEntretien(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntretienResponse> getEntretienById(@PathVariable String id) {
        return ResponseEntity.ok(entretienService.getEntretienById(id));
    }

    @GetMapping("/candidature/{candidatureId}")
    public ResponseEntity<List<EntretienResponse>> getEntretiensParCandidature(
            @PathVariable String candidatureId) {
        return ResponseEntity.ok(entretienService.getEntretiensParCandidature(candidatureId));
    }

    @GetMapping("/recruteur/{recruteurId}")
    public ResponseEntity<List<EntretienResponse>> getEntretiensParRecruteur(
            @PathVariable String recruteurId) {
        return ResponseEntity.ok(entretienService.getEntretiensParRecruteur(recruteurId));
    }

    @GetMapping("/candidat/{candidatId}")
    public ResponseEntity<List<EntretienResponse>> getEntretiensParCandidat(
            @PathVariable String candidatId,
            @RequestParam(defaultValue = "true") boolean confirmedOnly) {
        return ResponseEntity.ok(entretienService.getEntretiensParCandidat(candidatId, confirmedOnly));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntretienResponse> modifierEntretien(
            @PathVariable String id,
            @RequestBody CreateEntretienRequest request) {
        return ResponseEntity.ok(entretienService.modifierEntretien(id, request));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<EntretienResponse> ajouterFeedback(
            @PathVariable String id,
            @RequestBody FeedbackEntretienRequest request) {
        return ResponseEntity.ok(entretienService.ajouterFeedback(id, request));
    }

    @PatchMapping("/{id}/annuler")
    public ResponseEntity<Void> annulerEntretien(@PathVariable String id) {
        entretienService.annulerEntretien(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/realise")
    public ResponseEntity<Void> marquerRealise(@PathVariable String id) {
        entretienService.marquerRealise(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/confirmer")
    public ResponseEntity<EntretienResponse> confirmerPresenceCandidat(@PathVariable String id) {
        return ResponseEntity.ok(entretienService.confirmerPresenceCandidat(id));
    }
}