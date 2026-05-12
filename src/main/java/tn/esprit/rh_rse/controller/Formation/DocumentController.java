package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.entity.Formation.DocumentFormation;
import tn.esprit.rh_rse.entity.Formation.RenduExercice;
import tn.esprit.rh_rse.service.Formation.DocumentService;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class DocumentController {

    private final DocumentService documentService;

    // Récupérer tous les documents d'une formation
    @GetMapping("/formation/{formationId}")
    public ResponseEntity<?> getDocumentsByFormation(@PathVariable String formationId) {
        try {
            List<DocumentFormation> documents = documentService.getDocumentsByFormation(formationId);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Upload d'un document (formateur)
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("formationId") String formationId,
            @RequestParam("formateurId") String formateurId,
            @RequestParam("titre") String titre,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("type") String type) {
        try {
            DocumentFormation document = documentService.uploadDocument(
                    formationId, formateurId, titre, description, type, file);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur upload: " + e.getMessage());
        }
    }

    // Télécharger un document
    @GetMapping("/download/{documentId}")
    public ResponseEntity<?> downloadDocument(@PathVariable String documentId) {
        try {
            byte[] data = documentService.downloadDocument(documentId);
            DocumentFormation document = documentService.getDocumentById(documentId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", document.getFileName());

            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Document non trouvé: " + e.getMessage());
        }
    }

    // Supprimer un document
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur suppression: " + e.getMessage());
        }
    }

    // ==================== RENDUS ====================

    // Soumettre un rendu
    @PostMapping("/rendus")
    public ResponseEntity<?> soumettreRendu(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentId") String documentId,
            @RequestParam("employeId") String employeId,
            @RequestParam(value = "employeNom", required = false) String employeNom) {
        try {
            RenduExercice rendu = documentService.soumettreRendu(documentId, employeId, employeNom, file);
            return ResponseEntity.ok(rendu);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur soumission: " + e.getMessage());
        }
    }

    // Récupérer les rendus d'un employé
    @GetMapping("/rendus/employe/{employeId}")
    public ResponseEntity<?> getRendusByEmploye(@PathVariable String employeId) {
        try {
            List<RenduExercice> rendus = documentService.getRendusByEmploye(employeId);
            return ResponseEntity.ok(rendus);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Récupérer les rendus d'un document
    @GetMapping("/rendus/document/{documentId}")
    public ResponseEntity<?> getRendusByDocument(@PathVariable String documentId) {
        try {
            List<RenduExercice> rendus = documentService.getRendusByDocument(documentId);
            return ResponseEntity.ok(rendus);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    // Télécharger un rendu
    @GetMapping("/rendus/{renduId}/download")
    public ResponseEntity<?> downloadRendu(@PathVariable String renduId) {
        try {
            byte[] data = documentService.downloadRendu(renduId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "rendu_" + renduId + ".pdf");

            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Rendu non trouvé: " + e.getMessage());
        }
    }
}