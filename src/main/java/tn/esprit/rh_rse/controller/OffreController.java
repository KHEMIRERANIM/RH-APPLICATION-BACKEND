package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.entity.enums.CategorieOffre;
import tn.esprit.rh_rse.dto.response.UrgenceDto;
import tn.esprit.rh_rse.service.OffreService;
import tn.esprit.rh_rse.service.UrgenceAIService;

import java.util.List;

@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
public class OffreController {

    private final OffreService offreService;
    private final UrgenceAIService urgenceAIService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<List<Offre>> getAllActives(
            @RequestParam(required = false, name = "categorie") CategorieOffre categorie) {
        if (categorie != null) {
            return ResponseEntity.ok(offreService.getByCategorie(categorie));
        }
        return ResponseEntity.ok(offreService.getAllActives());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<Offre> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(offreService.getById(id));
    }

    @GetMapping("/{id}/urgence")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<UrgenceDto> evaluerUrgence(@PathVariable("id") String id) {
        return ResponseEntity.ok(urgenceAIService.evaluerUrgence(id));
    }

    @GetMapping("/partenaire/{idPartenaire}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Offre>> getByPartenaire(@PathVariable("idPartenaire") String idPartenaire) {
        return ResponseEntity.ok(offreService.getByPartenaire(idPartenaire));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Offre> creer(@RequestBody Offre offre) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offreService.creerOffre(offre));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Offre> modifier(
            @PathVariable("id") String id,
            @RequestBody Offre offre) {
        return ResponseEntity.ok(offreService.modifierOffre(id, offre));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable("id") String id) {
        offreService.supprimerOffre(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-statut")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Offre> toggleStatut(@PathVariable("id") String id) {
        return ResponseEntity.ok(offreService.toggleStatut(id));
    }
}