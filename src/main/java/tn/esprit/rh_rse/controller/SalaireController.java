package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.BulletinSalaireRequest;
import tn.esprit.rh_rse.dto.response.BulletinSalaireResponse;
import tn.esprit.rh_rse.service.SalaireService;

import java.util.List;

@RestController
@RequestMapping("/api/salaires")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class SalaireController {

    private final SalaireService salaireService;

    // ───────────────────────────────────────────────────
    //  ADMIN/RH — Créer un bulletin de salaire
    //  POST /api/salaires
    // ───────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<BulletinSalaireResponse> creerBulletin(
            @RequestBody BulletinSalaireRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(salaireService.creerBulletin(request));
    }

    // ───────────────────────────────────────────────────
    //  EMPLOYÉ — Voir tous ses bulletins
    //  GET /api/salaires/employe/{employeId}
    // ───────────────────────────────────────────────────
    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<BulletinSalaireResponse>> getBulletinsByEmploye(
            @PathVariable String employeId) {
        return ResponseEntity.ok(salaireService.getBulletinsByEmploye(employeId));
    }

    // ───────────────────────────────────────────────────
    //  EMPLOYÉ — Voir un bulletin par ID
    //  GET /api/salaires/{id}
    // ───────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<BulletinSalaireResponse> getBulletinById(
            @PathVariable String id) {
        return ResponseEntity.ok(salaireService.getBulletinById(id));
    }

    // ───────────────────────────────────────────────────
    //  EMPLOYÉ — Voir le bulletin d'un mois précis
    //  GET /api/salaires/employe/{employeId}/mois?mois=3&annee=2025
    // ───────────────────────────────────────────────────
    @GetMapping("/employe/{employeId}/mois")
    public ResponseEntity<BulletinSalaireResponse> getBulletinByMois(
            @PathVariable String employeId,
            @RequestParam int mois,
            @RequestParam int annee) {
        return ResponseEntity.ok(salaireService.getBulletinByMois(employeId, mois, annee));
    }

    // ───────────────────────────────────────────────────
    //  EMPLOYÉ — Voir tous les bulletins d'une année
    //  GET /api/salaires/employe/{employeId}/annee/{annee}
    // ───────────────────────────────────────────────────
    @GetMapping("/employe/{employeId}/annee/{annee}")
    public ResponseEntity<List<BulletinSalaireResponse>> getBulletinsByAnnee(
            @PathVariable String employeId,
            @PathVariable int annee) {
        return ResponseEntity.ok(salaireService.getBulletinsByAnnee(employeId, annee));
    }

    // ───────────────────────────────────────────────────
    //  ADMIN/RH — Modifier un bulletin
    //  PUT /api/salaires/{id}
    // ───────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<BulletinSalaireResponse> modifierBulletin(
            @PathVariable String id,
            @RequestBody BulletinSalaireRequest request) {
        return ResponseEntity.ok(salaireService.modifierBulletin(id, request));
    }

    // ───────────────────────────────────────────────────
    //  ADMIN/RH — Supprimer un bulletin
    //  DELETE /api/salaires/{id}
    // ───────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerBulletin(@PathVariable String id) {
        salaireService.supprimerBulletin(id);
        return ResponseEntity.noContent().build();
    }
}
