package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.Formation.FormationDTO;
import tn.esprit.rh_rse.dto.Formation.InscriptionFormationDTO;
import tn.esprit.rh_rse.service.Formation.FormationService;
import tn.esprit.rh_rse.service.Formation.InscriptionService;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")

@RestController
@RequestMapping("/api/formations")
@RequiredArgsConstructor
public class FormationController {

    private final FormationService formationService;
    private final InscriptionService inscriptionService;

    @GetMapping
    public ResponseEntity<List<FormationDTO>> getAllFormations() {
        return ResponseEntity.ok(formationService.getAllFormations());
    }

    @GetMapping("/disponibles")
    public ResponseEntity<List<FormationDTO>> getFormationsDisponibles() {
        return ResponseEntity.ok(formationService.getFormationsDisponibles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormationDTO> getFormationById(@PathVariable String id) {
        return ResponseEntity.ok(formationService.getFormationById(id));
    }

    @PostMapping
    public ResponseEntity<FormationDTO> createFormation(@RequestBody FormationDTO formationDTO) {
        return new ResponseEntity<>(formationService.createFormation(formationDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormationDTO> updateFormation(@PathVariable String id,
                                                        @RequestBody FormationDTO formationDTO) {
        return ResponseEntity.ok(formationService.updateFormation(id, formationDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFormation(@PathVariable String id) {
        formationService.deleteFormation(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<Void> toggleActive(@PathVariable String id) {
        formationService.toggleActive(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{formationId}/inscrire/{employeId}")
    public ResponseEntity<InscriptionFormationDTO> inscrireEmploye(
            @PathVariable String formationId,
            @PathVariable String employeId) {
        return new ResponseEntity<>(
                inscriptionService.inscrireEmploye(formationId, employeId),
                HttpStatus.CREATED
        );
    }

    @DeleteMapping("/inscriptions/{inscriptionId}")
    public ResponseEntity<Void> annulerInscription(
            @PathVariable String inscriptionId,
            @RequestParam String motif) {
        inscriptionService.annulerInscription(inscriptionId, motif);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/employe/{employeId}/inscriptions")
    public ResponseEntity<List<InscriptionFormationDTO>> getInscriptionsByEmploye(
            @PathVariable String employeId) {
        return ResponseEntity.ok(inscriptionService.getInscriptionsByEmploye(employeId));
    }

    @GetMapping("/{formationId}/inscriptions")
    public ResponseEntity<List<InscriptionFormationDTO>> getInscriptionsByFormation(
            @PathVariable String formationId) {
        return ResponseEntity.ok(inscriptionService.getInscriptionsByFormation(formationId));
    }

    @PatchMapping("/inscriptions/{inscriptionId}/presence")
    public ResponseEntity<Void> confirmerPresence(@PathVariable String inscriptionId) {
        inscriptionService.confirmerPresence(inscriptionId);
        return ResponseEntity.ok().build();
    }
}