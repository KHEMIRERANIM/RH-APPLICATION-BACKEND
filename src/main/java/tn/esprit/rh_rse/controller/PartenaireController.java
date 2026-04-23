package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Partenaire;
import tn.esprit.rh_rse.service.PartenaireService;

import java.util.List;

@RestController
@RequestMapping("/api/partenaires")
@RequiredArgsConstructor
public class PartenaireController {

    private final PartenaireService partenaireService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<List<Partenaire>> getAll() {
        return ResponseEntity.ok(partenaireService.getAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYE')")
    public ResponseEntity<Partenaire> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(partenaireService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Partenaire> creer(@RequestBody Partenaire partenaire) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(partenaireService.creerPartenaire(partenaire));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Partenaire> modifier(
            @PathVariable("id") String id,
            @RequestBody Partenaire partenaire) {
        return ResponseEntity.ok(partenaireService.modifierPartenaire(id, partenaire));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable("id") String id) {
        partenaireService.supprimerPartenaire(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-actif")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Partenaire> toggleActif(@PathVariable("id") String id) {
        return ResponseEntity.ok(partenaireService.toggleActif(id));
    }
}