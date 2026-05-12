package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.entity.Formation.Examen;
import tn.esprit.rh_rse.entity.Formation.Reponse;
import tn.esprit.rh_rse.entity.Formation.ResultatExamen;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.Formation.ExamenRepository;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.Formation.CertificationService;
import tn.esprit.rh_rse.service.Formation.ExamenService;
import tn.esprit.rh_rse.service.Formation.QrCodeFormationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/examens")
@RequiredArgsConstructor
@Slf4j
public class ExamenController {

    private final ExamenService examenService;
    private final CertificationService certificationService;
    private final UserRepository userRepository;
    private final ExamenRepository examenRepository;
    private final QrCodeFormationService qrCodeFormationService;  // ✅ AJOUTER

    // ==================== GESTION DES EXAMENS ====================

    @GetMapping("/formation/{formationId}")
    public ResponseEntity<List<Examen>> getExamensByFormation(@PathVariable String formationId) {
        return ResponseEntity.ok(examenService.getExamensByFormation(formationId));
    }

    @GetMapping("/{examenId}")
    public ResponseEntity<Examen> getExamenById(@PathVariable String examenId) {
        return ResponseEntity.ok(examenService.getExamenById(examenId));
    }

    @PostMapping
    public ResponseEntity<Examen> createExamen(@RequestBody Examen examen) {
        return new ResponseEntity<>(examenService.createExamen(examen), HttpStatus.CREATED);
    }

    @PutMapping("/{examenId}")
    public ResponseEntity<Examen> updateExamen(
            @PathVariable String examenId,
            @RequestBody Examen examen) {
        return ResponseEntity.ok(examenService.updateExamen(examenId, examen));
    }

    @DeleteMapping("/{examenId}")
    public ResponseEntity<Void> deleteExamen(@PathVariable String examenId) {
        examenService.deleteExamen(examenId);
        return ResponseEntity.noContent().build();
    }

    // ==================== VÉRIFICATION ACCÈS ====================

    @GetMapping("/formation/{formationId}/acces/{employeId}")
    public ResponseEntity<Map<String, Object>> verifierAccesExamen(
            @PathVariable String formationId,
            @PathVariable String employeId) {
        return ResponseEntity.ok(examenService.getExamenStatus(formationId, employeId));
    }

    @GetMapping("/formation/{formationId}/peutPasser/{employeId}")
    public ResponseEntity<Boolean> peutPasserExamen(
            @PathVariable String formationId,
            @PathVariable String employeId) {
        return ResponseEntity.ok(examenService.canAccessExamen(formationId, employeId));
    }

    // ==================== SOUMISSION ET RÉSULTATS ====================

    @PostMapping("/{examenId}/soumettre")
    public ResponseEntity<ResultatExamen> soumettreExamen(
            @PathVariable String examenId,
            @RequestParam String employeId,
            @RequestBody List<Reponse> reponses) {
        return new ResponseEntity<>(
                examenService.soumettreExamen(examenId, employeId, reponses),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{examenId}/resultats")
    public ResponseEntity<List<ResultatExamen>> getResultatsByExamen(@PathVariable String examenId) {
        return ResponseEntity.ok(examenService.getResultatsByExamen(examenId));
    }

    @GetMapping("/{examenId}/resultats/employe/{employeId}")
    public ResponseEntity<ResultatExamen> getResultatByEmploye(
            @PathVariable String examenId,
            @PathVariable String employeId) {
        ResultatExamen resultat = examenService.getResultatByEmploye(examenId, employeId);
        return resultat != null ? ResponseEntity.ok(resultat) : ResponseEntity.notFound().build();
    }

    // ✅ ENDPOINT POUR TÉLÉCHARGER LA CERTIFICATION PDF (CORRIGÉ)
    @GetMapping("/certification/{formationId}/download")
    public ResponseEntity<byte[]> telechargerCertification(
            @PathVariable String formationId,
            @RequestParam String employeId) {
        try {
            log.info("📥 Téléchargement certification - Formation: {}, Employé: {}", formationId, employeId);

            // Vérifier si l'employé a réussi l'examen
            List<Examen> examens = examenService.getExamensByFormation(formationId);
            boolean aReussi = false;
            Double note = null;
            String examenId = null;

            for (Examen examen : examens) {
                var resultat = examenService.getResultatByEmploye(examen.getId(), employeId);
                if (resultat != null && resultat.getNote() >= 10) {
                    aReussi = true;
                    note = resultat.getNote();
                    examenId = examen.getId();
                    break;
                }
            }

            if (!aReussi) {
                log.warn("❌ Certification non disponible - Employé {} n'a pas réussi", employeId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Récupérer les données
            User employe = userRepository.findById(employeId).orElse(null);
            Examen examen = examenRepository.findById(examenId).orElse(null);

            if (employe == null || examen == null) {
                log.error("❌ Données manquantes - Employé ou examen non trouvé");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String certificationId = "CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String dateCertification = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"));

            // ✅ Générer le QR Code pour la certification
            Map<String, Object> qrData = new HashMap<>();
            qrData.put("certificationId", certificationId);
            qrData.put("employeNom", employe.getNom());
            qrData.put("employePrenom", employe.getPrenom());
            qrData.put("formationTitre", examen.getTitre());
            qrData.put("note", note);
            qrData.put("date", dateCertification);

            String qrCodeBase64 = qrCodeFormationService.genererQRCodeFormation(qrData);

            // ✅ Appel avec 6 paramètres
            byte[] pdfBytes = certificationService.genererPDFCertification(employe, examen, note, certificationId, dateCertification, qrCodeBase64);

            if (pdfBytes == null) {
                log.error("❌ Erreur génération PDF");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certification_" + formationId + ".pdf\"")
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("❌ Erreur téléchargement certification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/employe/{employeId}/resultats")
    public ResponseEntity<List<ResultatExamen>> getResultatsByEmploye(@PathVariable String employeId) {
        return ResponseEntity.ok(examenService.getResultatsByEmploye(employeId));
    }

    @GetMapping("/{examenId}/statistiques")
    public ResponseEntity<Map<String, Object>> getStatistiquesExamen(@PathVariable String examenId) {
        return ResponseEntity.ok(examenService.getStatistiquesExamen(examenId));
    }
}