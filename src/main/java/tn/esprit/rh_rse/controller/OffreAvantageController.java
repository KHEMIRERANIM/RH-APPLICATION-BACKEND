package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.OffreAvantage;
import tn.esprit.rh_rse.entity.enums.CategorieOffreAvantage;
import tn.esprit.rh_rse.dto.response.UrgenceDto;
import tn.esprit.rh_rse.service.OffreAvantageService;
import tn.esprit.rh_rse.service.UrgenceAIService;

import java.util.List;

@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
public class OffreAvantageController {

    private final OffreAvantageService offreAvantageService;
    private final UrgenceAIService urgenceAIService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<List<OffreAvantage>> getAllActives(
            @RequestParam(required = false, name = "categorie") CategorieOffreAvantage categorie) {
        if (categorie != null) {
            return ResponseEntity.ok(offreAvantageService.getByCategorie(categorie));
        }
        return ResponseEntity.ok(offreAvantageService.getAllActives());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<OffreAvantage> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(offreAvantageService.getById(id));
    }

    @GetMapping("/{id}/urgence")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<UrgenceDto> evaluerUrgence(@PathVariable("id") String id) {
        return ResponseEntity.ok(urgenceAIService.evaluerUrgence(id));
    }

    @GetMapping("/partenaire/{idPartenaire}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OffreAvantage>> getByPartenaire(@PathVariable("idPartenaire") String idPartenaire) {
        return ResponseEntity.ok(offreAvantageService.getByPartenaire(idPartenaire));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OffreAvantage> creer(@RequestBody OffreAvantage offreAvantage) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offreAvantageService.creerOffreAvantage(offreAvantage));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OffreAvantage> modifier(
            @PathVariable("id") String id,
            @RequestBody OffreAvantage offreAvantage) {
        return ResponseEntity.ok(offreAvantageService.modifierOffreAvantage(id, offreAvantage));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable("id") String id) {
        offreAvantageService.supprimerOffreAvantage(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-statut")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OffreAvantage> toggleStatut(@PathVariable("id") String id) {
        return ResponseEntity.ok(offreAvantageService.toggleStatut(id));
    }
}