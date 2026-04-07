package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.ChangerStatutRequest;
import tn.esprit.rh_rse.dto.response.CandidatureResponse;
import tn.esprit.rh_rse.entity.enums.StatutCandidature;
import tn.esprit.rh_rse.service.CandidatureService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recrutement/candidatures")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CandidatureController {

    private final CandidatureService candidatureService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidatureResponse> postuler(
            @RequestParam String candidatId,
            @RequestParam String offreId,
            @RequestPart("cv") MultipartFile cv,
            @RequestPart(value = "lettre", required = false) MultipartFile lettre) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidatureService.postuler(candidatId, offreId, cv, lettre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidatureResponse> getCandidatureById(@PathVariable String id) {
        return ResponseEntity.ok(candidatureService.getCandidatureById(id));
    }

    @GetMapping("/offre/{offreId}")
    public ResponseEntity<List<CandidatureResponse>> getCandidaturesParOffre(@PathVariable String offreId) {
        return ResponseEntity.ok(candidatureService.getCandidaturesParOffre(offreId));
    }

    @GetMapping("/candidat/{candidatId}")
    public ResponseEntity<List<CandidatureResponse>> getMesCandidatures(@PathVariable String candidatId) {
        return ResponseEntity.ok(candidatureService.getMesCandidatures(candidatId));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<CandidatureResponse> changerStatut(
            @PathVariable String id,
            @RequestBody ChangerStatutRequest request) {
        return ResponseEntity.ok(candidatureService.changerStatut(id, request));
    }

    @PatchMapping("/{id}/notes")
    public ResponseEntity<CandidatureResponse> ajouterNotes(
            @PathVariable String id,
            @RequestParam String notes) {
        return ResponseEntity.ok(candidatureService.ajouterNotesRecruteur(id, notes));
    }

    @GetMapping("/kanban/{offreId}")
    public ResponseEntity<Map<StatutCandidature, List<CandidatureResponse>>> getKanban(
            @PathVariable String offreId) {
        return ResponseEntity.ok(candidatureService.getKanban(offreId));
    }

    @PostMapping("/{id}/test-langue")
    public ResponseEntity<CandidatureResponse> soumettreTestLangue(
            @PathVariable String id,
            @RequestParam Double scoreLangue) {
        return ResponseEntity.ok(candidatureService.soumettreTestLangue(id, scoreLangue));
    }

    @GetMapping("/top/{offreId}")
    public ResponseEntity<List<CandidatureResponse>> getTopCandidats(@PathVariable String offreId) {
        return ResponseEntity.ok(candidatureService.getTopCandidatsByScore(offreId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCandidature(@PathVariable String id) {
        candidatureService.deleteCandidature(id);
        return ResponseEntity.noContent().build();
    }
}