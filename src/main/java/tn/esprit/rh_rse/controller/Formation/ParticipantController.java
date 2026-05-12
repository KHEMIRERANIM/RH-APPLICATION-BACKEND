// ParticipantController.java
package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Formation.ParticipantInscription;
import tn.esprit.rh_rse.service.Formation.ParticipantService;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
@Slf4j
public class ParticipantController {

    private final ParticipantService participantService;

    // ✅ ENDPOINT POUR RÉCUPÉRER LES INSCRIPTIONS D'UN EMPLOYÉ
    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<ParticipantInscription>> getInscriptionsByEmploye(
            @PathVariable String employeId) {
        log.info("📋 Récupération des inscriptions pour l'employé: {}", employeId);
        List<ParticipantInscription> inscriptions = participantService.getInscriptionsByEmploye(employeId);
        return ResponseEntity.ok(inscriptions);
    }

    // ✅ ENDPOINT POUR RÉCUPÉRER LES INSCRIPTIONS PAR FORMATION
    @GetMapping("/formation/{formationId}")
    public ResponseEntity<List<ParticipantInscription>> getParticipantsByFormation(
            @PathVariable String formationId) {
        log.info("📋 Récupération des participants pour la formation: {}", formationId);
        return ResponseEntity.ok(participantService.getParticipantsByFormation(formationId));
    }

    // ✅ ENDPOINT POUR VALIDER LA PRÉSENCE
    @PutMapping("/formation/{formationId}/employe/{employeId}/presence")
    public ResponseEntity<ParticipantInscription> validerPresence(
            @PathVariable String formationId,
            @PathVariable String employeId,
            @RequestParam(required = false) String commentaire) {
        log.info("✅ Validation présence - Formation: {}, Employé: {}", formationId, employeId);
        return ResponseEntity.ok(participantService.validerPresence(formationId, employeId, commentaire));
    }

    // ✅ ENDPOINT POUR MARQUER ABSENT
    @PutMapping("/formation/{formationId}/employe/{employeId}/absent")
    public ResponseEntity<ParticipantInscription> marquerAbsent(
            @PathVariable String formationId,
            @PathVariable String employeId,
            @RequestParam(required = false) String commentaire) {
        log.info("❌ Marquage absent - Formation: {}, Employé: {}", formationId, employeId);
        return ResponseEntity.ok(participantService.marquerAbsent(formationId, employeId, commentaire));
    }

    // ✅ ENDPOINT POUR RÉINITIALISER LE STATUT
    @PutMapping("/formation/{formationId}/employe/{employeId}/reset")
    public ResponseEntity<ParticipantInscription> reinitialiserStatut(
            @PathVariable String formationId,
            @PathVariable String employeId) {
        log.info("🔄 Réinitialisation statut - Formation: {}, Employé: {}", formationId, employeId);
        return ResponseEntity.ok(participantService.reinitialiserStatut(formationId, employeId));
    }

    // ✅ ENDPOINT POUR ANNULER L'INSCRIPTION
    @DeleteMapping("/formation/{formationId}/employe/{employeId}")
    public ResponseEntity<Void> annulerInscription(
            @PathVariable String formationId,
            @PathVariable String employeId) {
        log.info("🗑️ Annulation inscription - Formation: {}, Employé: {}", formationId, employeId);
        participantService.annulerInscription(formationId, employeId);
        return ResponseEntity.noContent().build();
    }

    // ✅ ENDPOINT POUR LES STATISTIQUES
    @GetMapping("/formation/{formationId}/statistiques")
    public ResponseEntity<Object> getStatistiquesFormation(@PathVariable String formationId) {
        log.info("📊 Statistiques pour la formation: {}", formationId);
        return ResponseEntity.ok(participantService.getStatistiquesFormation(formationId));
    }
}