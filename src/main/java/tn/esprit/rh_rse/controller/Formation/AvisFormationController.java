package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Formation.Avis;
import tn.esprit.rh_rse.service.Formation.AvisFormationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/formation-avis")

@RequiredArgsConstructor
@Slf4j
public class AvisFormationController {

    private final AvisFormationService avisFormationService;

    @PostMapping
    public ResponseEntity<?> ajouterAvis(@RequestBody Map<String, Object> request) {
        try {
            String formationId = (String) request.get("formationId");
            String employeId = (String) request.get("employeId");
            Integer note = (Integer) request.get("note");
            String titre = (String) request.get("titre");
            String commentaire = (String) request.get("commentaire");

            Avis avis = avisFormationService.ajouterAvis(formationId, employeId, note, titre, commentaire);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Avis ajouté avec succès");
            response.put("avis", avis);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping("/{avisId}/valider")
    public ResponseEntity<Avis> validerAvis(@PathVariable String avisId) {
        return ResponseEntity.ok(avisFormationService.validerAvis(avisId));
    }

    @DeleteMapping("/{avisId}")
    public ResponseEntity<Void> supprimerAvis(@PathVariable String avisId) {
        avisFormationService.supprimerAvis(avisId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/en-attente")
    public ResponseEntity<List<Avis>> getAvisEnAttente() {
        log.info("📋 GET /api/avis/en-attente");
        return ResponseEntity.ok(avisFormationService.getAvisEnAttente());
    }
    // Dans AvisController.java
    @GetMapping("/formation/{formationId}/employe/{employeId}")
    public ResponseEntity<Avis> getAvisByFormationAndEmploye(
            @PathVariable String formationId,
            @PathVariable String employeId) {
        return ResponseEntity.ok(avisFormationService.getAvisByFormationAndEmploye(formationId, employeId));
    }

    @GetMapping("/formation/{formationId}")
    public ResponseEntity<List<Avis>> getAvisByFormation(@PathVariable String formationId) {
        return ResponseEntity.ok(avisFormationService.getAvisByFormation(formationId));
    }

    @GetMapping("/formation/{formationId}/statistiques")
    public ResponseEntity<Map<String, Object>> getStatistiquesAvis(@PathVariable String formationId) {
        return ResponseEntity.ok(avisFormationService.getStatistiquesAvis(formationId));
    }

    @GetMapping("/formation/{formationId}/dejaAvis/{employeId}")
    public ResponseEntity<Boolean> aDejaAvis(
            @PathVariable String formationId,
            @PathVariable String employeId) {
        return ResponseEntity.ok(avisFormationService.aDejaAvis(formationId, employeId));
    }
}