// DocumentService.java
package tn.esprit.rh_rse.service.Formation;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.entity.Formation.DocumentFormation;
import tn.esprit.rh_rse.entity.Formation.RenduExercice;
import tn.esprit.rh_rse.repository.Formation.DocumentRepository;
import tn.esprit.rh_rse.repository.Formation.RenduExerciceRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final RenduExerciceRepository renduExerciceRepository;

    @Value("${upload.directory:uploads}")
    private String uploadDirectory;

    public DocumentFormation uploadDocument(
            String formationId,
            String formateurId,
            String titre,
            String description,
            String type,
            MultipartFile file) throws IOException {

        // Créer le répertoire si nécessaire
        Path uploadPath = Paths.get(uploadDirectory, "documents", formationId);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Générer un nom de fichier unique
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + extension;

        // Sauvegarder le fichier
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // Créer l'entité
        DocumentFormation document = new DocumentFormation();
        document.setFormationId(formationId);
        document.setFormateurId(formateurId);
        document.setTitre(titre);
        document.setDescription(description);
        document.setType(type);
        document.setFileName(originalFileName);
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setFilePath(filePath.toString());

        return documentRepository.save(document);
    }

    public List<DocumentFormation> getDocumentsByFormation(String formationId) {
        return documentRepository.findByFormationId(formationId);
    }

    public DocumentFormation getDocumentById(String documentId) {
        return documentRepository.findById(documentId).orElse(null);
    }

    public void deleteDocument(String documentId) throws IOException {
        DocumentFormation document = documentRepository.findById(documentId).orElse(null);
        if (document != null) {
            // Supprimer le fichier physique
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);

            // Supprimer l'entrée en base
            documentRepository.deleteById(documentId);

            // Supprimer aussi les rendus associés
            List<RenduExercice> rendus = renduExerciceRepository.findByDocumentId(documentId);
            for (RenduExercice rendu : rendus) {
                Path renduPath = Paths.get(rendu.getFilePath());
                Files.deleteIfExists(renduPath);
                renduExerciceRepository.delete(rendu);
            }
        }
    }

    public byte[] downloadDocument(String documentId) throws IOException {
        DocumentFormation document = documentRepository.findById(documentId).orElse(null);
        if (document != null) {
            Path filePath = Paths.get(document.getFilePath());
            return Files.readAllBytes(filePath);
        }
        return null;
    }

    // Méthodes pour les rendus
    public RenduExercice soumettreRendu(String documentId, String employeId, String employeNom, MultipartFile file) throws IOException {
        DocumentFormation document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            throw new RuntimeException("Document non trouvé");
        }

        // Vérifier si l'employé a déjà rendu cet exercice
        RenduExercice existingRendu = renduExerciceRepository.findByDocumentIdAndEmployeId(documentId, employeId);
        if (existingRendu != null) {
            throw new RuntimeException("Vous avez déjà rendu cet exercice");
        }

        // Créer le répertoire pour les rendus
        Path uploadPath = Paths.get(uploadDirectory, "rendus", documentId);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Générer un nom de fichier unique
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String fileName = employeId + "_" + System.currentTimeMillis() + extension;

        // Sauvegarder le fichier
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // Créer l'entité
        RenduExercice rendu = new RenduExercice();
        rendu.setDocumentId(documentId);
        rendu.setEmployeId(employeId);
        rendu.setEmployeNom(employeNom);
        rendu.setFileName(originalFileName);
        rendu.setFileType(file.getContentType());
        rendu.setFileSize(file.getSize());
        rendu.setFilePath(filePath.toString());

        return renduExerciceRepository.save(rendu);
    }

    public List<RenduExercice> getRendusByEmploye(String employeId) {
        return renduExerciceRepository.findByEmployeId(employeId);
    }

    public List<RenduExercice> getRendusByDocument(String documentId) {
        return renduExerciceRepository.findByDocumentId(documentId);
    }

    public byte[] downloadRendu(String renduId) throws IOException {
        RenduExercice rendu = renduExerciceRepository.findById(renduId).orElse(null);
        if (rendu != null) {
            Path filePath = Paths.get(rendu.getFilePath());
            return Files.readAllBytes(filePath);
        }
        return null;
    }

    public RenduExercice noterRendu(String renduId, Double note, String commentaire) {
        RenduExercice rendu = renduExerciceRepository.findById(renduId).orElse(null);
        if (rendu != null) {
            rendu.setNote(note);
            rendu.setCommentaire(commentaire);
            rendu.setStatut(note >= 10 ? "NOTE" : "REJETE");
            return renduExerciceRepository.save(rendu);
        }
        return null;
    }
}