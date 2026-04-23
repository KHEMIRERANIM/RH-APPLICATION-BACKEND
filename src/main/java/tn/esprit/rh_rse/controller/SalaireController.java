package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.BulletinSalaireRequest;
import tn.esprit.rh_rse.dto.response.BulletinSalaireResponse;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.UserRepository;
import tn.esprit.rh_rse.service.PdfGeneratorService;
import tn.esprit.rh_rse.service.SalaireService;

import java.util.List;

@RestController
@RequestMapping("/api/salaires")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class SalaireController {

    private final SalaireService salaireService;
    private final UserRepository userRepository;
    private final PdfGeneratorService pdfGeneratorService;

    @PostMapping
    public ResponseEntity<BulletinSalaireResponse> creerBulletin(
            @RequestBody BulletinSalaireRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(salaireService.creerBulletin(request));
    }

    @GetMapping("/employe/{employeId}")
    public ResponseEntity<List<BulletinSalaireResponse>> getBulletinsByEmploye(
            @PathVariable String employeId) {
        return ResponseEntity.ok(salaireService.getBulletinsByEmploye(employeId));
    }

    @GetMapping
    public ResponseEntity<List<BulletinSalaireResponse>> getAllBulletins() {
        return ResponseEntity.ok(salaireService.getAllBulletins());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BulletinSalaireResponse> getBulletinById(
            @PathVariable String id) {
        return ResponseEntity.ok(salaireService.getBulletinById(id));
    }

    @GetMapping("/employe/{employeId}/mois")
    public ResponseEntity<BulletinSalaireResponse> getBulletinByMois(
            @PathVariable String employeId,
            @RequestParam int mois,
            @RequestParam int annee) {
        return ResponseEntity.ok(salaireService.getBulletinByMois(employeId, mois, annee));
    }

    @GetMapping("/employe/{employeId}/annee/{annee}")
    public ResponseEntity<List<BulletinSalaireResponse>> getBulletinsByAnnee(
            @PathVariable String employeId,
            @PathVariable int annee) {
        return ResponseEntity.ok(salaireService.getBulletinsByAnnee(employeId, annee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BulletinSalaireResponse> modifierBulletin(
            @PathVariable String id,
            @RequestBody BulletinSalaireRequest request) {
        return ResponseEntity.ok(salaireService.modifierBulletin(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerBulletin(@PathVariable String id) {
        salaireService.supprimerBulletin(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> telechargerPDF(@PathVariable String id) {
        try {
            System.out.println("=== DÉBUT PDF ===");
            System.out.println("1. ID reçu: " + id);

            BulletinSalaireResponse bulletin = salaireService.getBulletinById(id);
            System.out.println("2. Bulletin trouvé - ID: " + bulletin.getId() + ", EmployeId: " + bulletin.getEmployeId());

            User employe = userRepository.findById(bulletin.getEmployeId()).orElse(null);
            System.out.println("3. Employe trouvé: " + (employe != null ? employe.getEmail() : "NULL"));

            if (employe == null) {
                System.err.println("❌ Employé non trouvé pour ID: " + bulletin.getEmployeId());
                return ResponseEntity.notFound().build();
            }

            System.out.println("4. Génération du PDF...");
            byte[] pdf = pdfGeneratorService.genererBulletinPDF(bulletin, employe);
            System.out.println("5. PDF généré, taille: " + pdf.length + " bytes");

            System.out.println("=== FIN PDF ===");

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=bulletin_" + id + ".pdf")
                    .body(pdf);

        } catch (Exception e) {
            System.err.println("❌ ERREUR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}