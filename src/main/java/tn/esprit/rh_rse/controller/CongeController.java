package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.dto.request.DemandeCongeRequest;
import tn.esprit.rh_rse.dto.request.ValidationCongeRequest;
import tn.esprit.rh_rse.dto.response.DemandeCongeResponse;
import tn.esprit.rh_rse.dto.response.SoldeCongeResponse;
import tn.esprit.rh_rse.entity.enums.TypeConge;
import tn.esprit.rh_rse.service.CongeService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/conges")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class CongeController {

    private final CongeService congeService;

    @GetMapping
    public ResponseEntity<List<DemandeCongeResponse>> getAllDemandes() {
        System.out.println("📋 GET /api/conges - Récupération de toutes les demandes");
        return ResponseEntity.ok(congeService.getAllDemandes());
    }

    @PostMapping
    public ResponseEntity<DemandeCongeResponse> soumettreDemande(
            @RequestBody DemandeCongeRequest request) {
        System.out.println("📝 POST /api/conges - Soumission demande sans fichier");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(congeService.soumettreDemande(request));
    }

    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<DemandeCongeResponse>> getMesDemandes(
            @PathVariable String employeId) {
        System.out.println("👤 GET /api/conges/employe/" + employeId);
        return ResponseEntity.ok(congeService.getMesDemandes(employeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeCongeResponse> getDemandeById(
            @PathVariable String id) {
        System.out.println("🔍 GET /api/conges/" + id);
        return ResponseEntity.ok(congeService.getDemandeById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> annulerDemande(@PathVariable String id) {
        System.out.println("❌ DELETE /api/conges/" + id + " - Annulation demande");
        congeService.annulerDemande(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/solde/{employeId}")
    public ResponseEntity<SoldeCongeResponse> getSolde(
            @PathVariable String employeId) {
        System.out.println("💰 GET /api/conges/solde/" + employeId);
        return ResponseEntity.ok(congeService.getSoldeConge(employeId));
    }

    @GetMapping("/manager/{managerId}/en-attente")
    public ResponseEntity<List<DemandeCongeResponse>> getDemandesEnAttente(
            @PathVariable String managerId) {
        System.out.println("⏳ GET /api/conges/manager/" + managerId + "/en-attente");
        return ResponseEntity.ok(congeService.getDemandesEnAttente(managerId));
    }

    @GetMapping("/manager/{managerId}/toutes")
    public ResponseEntity<List<DemandeCongeResponse>> getToutesDemandesEquipe(
            @PathVariable String managerId) {
        System.out.println("📊 GET /api/conges/manager/" + managerId + "/toutes");
        return ResponseEntity.ok(congeService.getToutesDemandesEquipe(managerId));
    }

    @PatchMapping("/{id}/valider")
    public ResponseEntity<DemandeCongeResponse> validerDemande(
            @PathVariable String id,
            @RequestBody ValidationCongeRequest request) {
        System.out.println("✅ PATCH /api/conges/" + id + "/valider");
        System.out.println("   Statut: " + request.getStatut());
        System.out.println("   Commentaire: " + request.getCommentaireManager());
        return ResponseEntity.ok(congeService.validerDemande(id, request));
    }

    @GetMapping("/manager/{managerId}/alertes")
    public ResponseEntity<Map<String, Object>> detecterTendances(
            @PathVariable String managerId) {
        System.out.println("⚠️ GET /api/conges/manager/" + managerId + "/alertes");
        return ResponseEntity.ok(congeService.detecterTendances(managerId));
    }

    @GetMapping("/en-attente")
    public ResponseEntity<List<DemandeCongeResponse>> getAllDemandesEnAttente() {
        System.out.println("⏳ GET /api/conges/en-attente");
        return ResponseEntity.ok(congeService.getAllDemandesEnAttente());
    }

    @DeleteMapping("/supprimer/{id}")
    public ResponseEntity<Void> supprimerDemande(@PathVariable String id) {
        System.out.println("🗑️ DELETE /api/conges/supprimer/" + id + " - Suppression définitive");
        congeService.supprimerDemande(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<DemandeCongeResponse> modifierDemande(
            @PathVariable String id,
            @RequestBody DemandeCongeRequest request) {
        System.out.println("✏️ PUT /api/conges/" + id + " - Modification demande");
        return ResponseEntity.ok(congeService.modifierDemande(id, request));
    }

    // ───────────────────────────────────────────────────
    //  EMPLOYÉ — Soumettre une demande avec fichier
    //  POST /api/conges/with-file
    // ───────────────────────────────────────────────────
    @PostMapping("/with-file")
    public ResponseEntity<DemandeCongeResponse> soumettreDemandeWithFile(
            @RequestParam("employeId") String employeId,
            @RequestParam("managerId") String managerId,
            @RequestParam("dateDebut") String dateDebut,
            @RequestParam("dateFin") String dateFin,
            @RequestParam("motif") String motif,
            @RequestParam("type") String type,
            @RequestParam(value = "document", required = false) MultipartFile document) {

        System.out.println("=== 📝 SOUMISSION AVEC FICHIER ===");
        System.out.println("  employeId: " + employeId);
        System.out.println("  managerId: " + managerId);
        System.out.println("  dateDebut reçue: " + dateDebut);
        System.out.println("  dateFin reçue: " + dateFin);
        System.out.println("  motif: " + motif);
        System.out.println("  type: " + type);
        System.out.println("  document: " + (document != null ? document.getOriginalFilename() + " (" + document.getSize() + " bytes)" : "null"));

        try {
            // Formater la date depuis le format du frontend
            LocalDate parsedDateDebut = parseDate(dateDebut);
            LocalDate parsedDateFin = parseDate(dateFin);

            System.out.println("  dateDebut parsée: " + parsedDateDebut);
            System.out.println("  dateFin parsée: " + parsedDateFin);

            DemandeCongeRequest request = new DemandeCongeRequest();
            request.setEmployeId(employeId);
            request.setManagerId(managerId);
            request.setDateDebut(parsedDateDebut);
            request.setDateFin(parsedDateFin);
            request.setMotif(motif);
            request.setType(TypeConge.valueOf(type));

            System.out.println("  ✅ Requête construite, appel du service...");

            DemandeCongeResponse response = congeService.soumettreDemandeWithFile(request, document);

            System.out.println("  ✅ Succès! Demande créée avec ID: " + response.getId());
            System.out.println("=== FIN SOUMISSION ===");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            System.err.println("  ❌ ERREUR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // Méthode utilitaire pour parser la date du frontend
    private LocalDate parseDate(String dateString) {
        try {
            // Essayer le format ISO (YYYY-MM-DD)
            return LocalDate.parse(dateString);
        } catch (Exception e1) {
            try {
                // Essayer le format: "EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd yyyy HH:mm:ss", Locale.ENGLISH);
                String datePart = dateString.substring(0, 24);
                LocalDate date = LocalDate.parse(datePart, formatter);
                System.out.println("  ✅ Date parsée avec format anglais: " + date);
                return date;
            } catch (Exception e2) {
                try {
                    // Essayer le format: "dd/MM/yyyy"
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    return LocalDate.parse(dateString, formatter);
                } catch (Exception e3) {
                    throw new RuntimeException("Impossible de parser la date: " + dateString);
                }
            }
        }
    }

    @GetMapping("/uploads/{fileName}")
    public ResponseEntity<Resource> getDocument(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("./uploads/conges/").resolve(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}