package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.DemandeCongeRequest;
import tn.esprit.rh_rse.dto.request.ValidationCongeRequest;
import tn.esprit.rh_rse.dto.response.DemandeCongeResponse;
import tn.esprit.rh_rse.dto.response.SoldeCongeResponse;
import tn.esprit.rh_rse.service.CongeService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conges")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CongeController {

    private final CongeService congeService;

    // ───────────────────────────────────────────────────
    //  EMPLOYÉ — Soumettre une demande
    //  POST /api/conges
    // ───────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<DemandeCongeResponse> soumettreDemande(
            @RequestBody DemandeCongeRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(congeService.soumettreDemande(request));
    }

    // ───────────────────────────────────────────────────
    //  EMPLOYÉ — Voir toutes mes demandes
    //  GET /api/conges/employe/{employeId}
    // ───────────────────────────────────────────────────
    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<DemandeCongeResponse>> getMesDemandes(
            @PathVariable String employeId) {
        return ResponseEntity.ok(congeService.getMesDemandes(employeId));
    }

    // ───────────────────────────────────────────────────
    //  EMPLOYÉ / MANAGER — Voir une demande par ID
    //  GET /api/conges/{id}
    // ───────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<DemandeCongeResponse> getDemandeById(
            @PathVariable String id) {
        return ResponseEntity.ok(congeService.getDemandeById(id));
    }

    // ───────────────────────────────────────────────────
    //  EMPLOYÉ — Annuler une demande (EN_ATTENTE seulement)
    //  DELETE /api/conges/{id}
    // ───────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> annulerDemande(@PathVariable String id) {
        congeService.annulerDemande(id);
        return ResponseEntity.noContent().build();
    }

    // ───────────────────────────────────────────────────
    //  EMPLOYÉ — Consulter son solde de congés
    //  GET /api/conges/solde/{employeId}
    // ───────────────────────────────────────────────────
    @GetMapping("/solde/{employeId}")
    public ResponseEntity<SoldeCongeResponse> getSolde(
            @PathVariable String employeId) {
        return ResponseEntity.ok(congeService.getSoldeConge(employeId));
    }

    // ───────────────────────────────────────────────────
    //  MANAGER — Voir les demandes EN_ATTENTE de son équipe
    //  GET /api/conges/manager/{managerId}/en-attente
    // ───────────────────────────────────────────────────
    @GetMapping("/manager/{managerId}/en-attente")
    public ResponseEntity<List<DemandeCongeResponse>> getDemandesEnAttente(
            @PathVariable String managerId) {
        return ResponseEntity.ok(congeService.getDemandesEnAttente(managerId));
    }

    // ───────────────────────────────────────────────────
    //  MANAGER — Voir toutes les demandes de son équipe
    //  GET /api/conges/manager/{managerId}/toutes
    // ───────────────────────────────────────────────────
    @GetMapping("/manager/{managerId}/toutes")
    public ResponseEntity<List<DemandeCongeResponse>> getToutesDemandesEquipe(
            @PathVariable String managerId) {
        return ResponseEntity.ok(congeService.getToutesDemandesEquipe(managerId));
    }

    // ───────────────────────────────────────────────────
    //  MANAGER — Approuver ou Refuser une demande
    //  PATCH /api/conges/{id}/valider
    // ───────────────────────────────────────────────────
    @PatchMapping("/{id}/valider")
    public ResponseEntity<DemandeCongeResponse> validerDemande(
            @PathVariable String id,
            @RequestBody ValidationCongeRequest request) {
        return ResponseEntity.ok(congeService.validerDemande(id, request));
    }

    // ───────────────────────────────────────────────────
    //  MANAGER — Détecter les tendances suspectes
    //  GET /api/conges/manager/{managerId}/alertes
    // ───────────────────────────────────────────────────
    @GetMapping("/manager/{managerId}/alertes")
    public ResponseEntity<Map<String, Object>> detecterTendances(
            @PathVariable String managerId) {
        return ResponseEntity.ok(congeService.detecterTendances(managerId));
    }
}
