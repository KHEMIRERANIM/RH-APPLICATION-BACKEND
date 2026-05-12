// FormateurController.java
package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Formation.Formateur;
import tn.esprit.rh_rse.service.Formation.FormateurService;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")  // ← AJOUTEZ CETTE LIGNE EXACTEMENT COMME DANS FormationController
@RestController
@RequestMapping("/api/formateurs")
@RequiredArgsConstructor
public class FormateurController {

    private final FormateurService formateurService;

    @GetMapping
    public ResponseEntity<List<Formateur>> getAllFormateurs() {
        return ResponseEntity.ok(formateurService.getAllFormateurs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Formateur> getFormateurById(@PathVariable String id) {
        return ResponseEntity.ok(formateurService.getFormateurById(id));
    }

    @PostMapping
    public ResponseEntity<Formateur> createFormateur(@RequestBody Formateur formateur) {
        return new ResponseEntity<>(formateurService.createFormateur(formateur), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Formateur> updateFormateur(@PathVariable String id, @RequestBody Formateur formateur) {
        return ResponseEntity.ok(formateurService.updateFormateur(id, formateur));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFormateur(@PathVariable String id) {
        formateurService.deleteFormateur(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/formations/{formationId}")
    public ResponseEntity<Formateur> assignerFormation(
            @PathVariable String id,
            @PathVariable String formationId) {
        return ResponseEntity.ok(formateurService.assignerFormation(id, formationId));
    }

    @DeleteMapping("/{id}/formations/{formationId}")
    public ResponseEntity<Formateur> retirerFormation(
            @PathVariable String id,
            @PathVariable String formationId) {
        return ResponseEntity.ok(formateurService.retirerFormation(id, formationId));
    }

    @GetMapping("/{id}/formations")
    public ResponseEntity<List<?>> getFormationsByFormateur(@PathVariable String id) {
        return ResponseEntity.ok(formateurService.getFormationsByFormateur(id));
    }
}