package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.rh_rse.entity.Formation.*;
import tn.esprit.rh_rse.repository.Formation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FormateurService {

    private final FormateurRepository formateurRepository;
    private final DocumentFormationRepository documentRepository;
    private final ExamenRepository examenRepository;
    private final InscriptionFormationRepository inscriptionRepository;
    private final FormationRepository formationRepository;

    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    // ==================== GESTION DES FORMATEURS ====================

    public List<Formateur> getAllFormateurs() {
        return formateurRepository.findAll();
    }

    public Formateur getFormateurById(String id) {
        return formateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formateur non trouvé"));
    }

    public Formateur createFormateur(Formateur formateur) {
        if (formateurRepository.findByEmail(formateur.getEmail()).isPresent()) {
            throw new RuntimeException("Email déjà utilisé");
        }
        formateur.setCreatedAt(LocalDateTime.now());
        formateur.setUpdatedAt(LocalDateTime.now());
        return formateurRepository.save(formateur);
    }

    public Formateur updateFormateur(String id, Formateur formateurDetails) {
        Formateur formateur = getFormateurById(id);
        formateur.setNom(formateurDetails.getNom());
        formateur.setPrenom(formateurDetails.getPrenom());
        formateur.setEmail(formateurDetails.getEmail());
        formateur.setTelephone(formateurDetails.getTelephone());
        formateur.setSpecialite(formateurDetails.getSpecialite());
        formateur.setBio(formateurDetails.getBio());
        formateur.setPhoto(formateurDetails.getPhoto());
        formateur.setStatus(formateurDetails.getStatus());
        formateur.setUpdatedAt(LocalDateTime.now());
        return formateurRepository.save(formateur);
    }

    public void deleteFormateur(String id) {
        formateurRepository.deleteById(id);
    }

    // ==================== GESTION DES FORMATIONS ASSIGNEES ====================

    public Formateur assignerFormation(String formateurId, String formationId) {
        Formateur formateur = getFormateurById(formateurId);
        if (formateur.getFormationsAssignees() == null) {
            formateur.setFormationsAssignees(new java.util.ArrayList<>());
        }
        if (!formateur.getFormationsAssignees().contains(formationId)) {
            formateur.getFormationsAssignees().add(formationId);
            formateur.setUpdatedAt(LocalDateTime.now());
            formateurRepository.save(formateur);

            // Mettre à jour la formation avec l'ID du formateur
            Formation formation = formationRepository.findById(formationId).orElse(null);
            if (formation != null) {
                formation.setFormateurId(formateurId);
                formation.setFormateur(formateur.getNom() + " " + formateur.getPrenom());
                formationRepository.save(formation);
            }
        }
        return formateur;
    }

    public Formateur retirerFormation(String formateurId, String formationId) {
        Formateur formateur = getFormateurById(formateurId);
        if (formateur.getFormationsAssignees() != null) {
            formateur.getFormationsAssignees().remove(formationId);
            formateur.setUpdatedAt(LocalDateTime.now());
            formateurRepository.save(formateur);

            // Retirer le formateur de la formation
            Formation formation = formationRepository.findById(formationId).orElse(null);
            if (formation != null) {
                formation.setFormateurId(null);
                formation.setFormateur(null);
                formationRepository.save(formation);
            }
        }
        return formateur;
    }
    public List<Formation> getFormationsByFormateur(String formateurId) {
        // D'abord essayer par formateurId
        List<Formation> formations = formationRepository.findByFormateurId(formateurId);

        // Si vide, chercher par nom du formateur
        if (formations.isEmpty()) {
            Formateur formateur = getFormateurById(formateurId);
            String nomComplet1 = formateur.getNom() + " " + formateur.getPrenom();
            String nomComplet2 = formateur.getPrenom() + " " + formateur.getNom();

            formations = formationRepository.findAll().stream()
                    .filter(f -> f.getFormateur() != null && (
                            f.getFormateur().equals(nomComplet1) ||
                                    f.getFormateur().equals(nomComplet2) ||
                                    f.getFormateur().contains(formateur.getNom()) ||
                                    f.getFormateur().contains(formateur.getPrenom())
                    ))
                    .collect(java.util.stream.Collectors.toList());
        }

        return formations;
    }
    /*public List<Formation> getFormationsByFormateur(String formateurId) {
        Formateur formateur = getFormateurById(formateurId);
        if (formateur.getFormationsAssignees() == null) {
            return new java.util.ArrayList<>();

        return formationRepository.findAllById(formateur.getFormationsAssignees());
    }
*/
    // ==================== GESTION DES DOCUMENTS ====================

    // Dans FormateurService.java, modifiez la méthode qui crée le document

    public DocumentFormation uploadDocument(
            String formationId,
            String formateurId,
            String titre,
            String description,
            String type,
            MultipartFile file) throws IOException {

        // Créer le répertoire si nécessaire


        // Générer un nom de fichier unique
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + extension;


        // Créer l'URL pour accéder au fichier
        String url = "/api/documents/download/" + fileName; // ou l'URL complète

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

        return documentRepository.save(document);
    }

    public List<DocumentFormation> getDocumentsByFormation(String formationId) {
        return documentRepository.findByFormationId(formationId);
    }

    public List<DocumentFormation> getDocumentsByFormateur(String formateurId) {
        return documentRepository.findByFormateurId(formateurId);
    }

    public void deleteDocument(String documentId) {
        documentRepository.deleteById(documentId);
    }

    // ==================== GESTION DES EXAMENS ====================

    public Examen createExamen(Examen examen) {
        examen.setId(UUID.randomUUID().toString());
        examen.setCreatedAt(LocalDateTime.now());
        examen.setUpdatedAt(LocalDateTime.now());
        return examenRepository.save(examen);
    }

    public Examen updateExamen(String id, Examen examenDetails) {
        Examen examen = examenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        examen.setTitre(examenDetails.getTitre());
        examen.setDescription(examenDetails.getDescription());
        examen.setDureeMinutes(examenDetails.getDureeMinutes());
        examen.setDateLimite(examenDetails.getDateLimite());
        examen.setQuestions(examenDetails.getQuestions());
        examen.setUpdatedAt(LocalDateTime.now());

        return examenRepository.save(examen);
    }

    public List<Examen> getExamensByFormation(String formationId) {
        return examenRepository.findByFormationId(formationId);
    }

    public void deleteExamen(String id) {
        examenRepository.deleteById(id);
    }

    // ==================== VALIDATION DES FORMATIONS ====================

    public InscriptionFormation validerFormation(String inscriptionId, String formateurId, String commentaire) {
        InscriptionFormation inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        inscription.setStatut("VALIDE");
        inscription.setDateValidation(LocalDateTime.now());
        inscription.setFormateurValidateur(formateurId);
        inscription.setValidationCommentaire(commentaire);

        return inscriptionRepository.save(inscription);
    }

    public InscriptionFormation evaluerFormation(String inscriptionId, Double note, String commentaire) {
        InscriptionFormation inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        inscription.setNote(note);
        inscription.setCommentaire(commentaire);
        inscription.setStatut(note >= 10 ? "VALIDE" : "ECHEC");
        inscription.setDateValidation(LocalDateTime.now());

        return inscriptionRepository.save(inscription);
    }

    public InscriptionFormation soumettreExamen(String inscriptionId, String examenId, List<Reponse> reponses) {
        InscriptionFormation inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé"));

        // Calculer la note
        double note = calculerNote(examen, reponses);

        ResultatExamen resultat = new ResultatExamen();
        resultat.setId(UUID.randomUUID().toString());
        resultat.setExamenId(examenId);
        resultat.setNote(note);
        resultat.setReponses(reponses);
        resultat.setSubmittedAt(LocalDateTime.now());
        resultat.setValide(note >= 10);

        if (inscription.getResultatsExamens() == null) {
            inscription.setResultatsExamens(new java.util.ArrayList<>());
        }
        inscription.getResultatsExamens().add(resultat);

        // Mettre à jour la note globale
        double noteGlobale = inscription.getResultatsExamens().stream()
                .mapToDouble(ResultatExamen::getNote)
                .average()
                .orElse(0);

        inscription.setNote(noteGlobale);
        inscription.setStatut(noteGlobale >= 10 ? "VALIDE" : "EN_COURS");

        if (noteGlobale >= 10) {
            inscription.setDateValidation(LocalDateTime.now());
        }

        return inscriptionRepository.save(inscription);
    }

    private double calculerNote(Examen examen, List<Reponse> reponses) {
        if (examen.getQuestions() == null || examen.getQuestions().isEmpty()) {
            return 0;
        }

        double totalPoints = 0;
        double pointsObtenus = 0;

        for (Question question : examen.getQuestions()) {
            totalPoints += question.getPoints();

            Reponse reponse = reponses.stream()
                    .filter(r -> r.getQuestionId().equals(question.getId()))
                    .findFirst()
                    .orElse(null);

            if (reponse != null) {
                if (question.getType().equals("QCM") && question.getOptions() != null) {
                    OptionQuestion optionCorrecte = question.getOptions().stream()
                            .filter(OptionQuestion::getEstCorrect)
                            .findFirst()
                            .orElse(null);

                    if (optionCorrecte != null && reponse.getReponse().equals(optionCorrecte.getId())) {
                        pointsObtenus += question.getPoints();
                        reponse.setPointsObtenus(question.getPoints());
                    } else {
                        reponse.setPointsObtenus(0);
                    }
                } else if (question.getType().equals("TEXTE")) {
                    // ✅ CORRECTION: Vérifier le type de pointsObtenus
                    Double points = reponse.getPointsObtenus();
                    if (points != null) {
                        pointsObtenus += points;
                        reponse.setPointsObtenus(points);
                    } else {
                        reponse.setPointsObtenus(0);
                    }
                } else if (question.getType().equals("CODE")) {
                    // ✅ CORRECTION: Vérifier le type de pointsObtenus
                    Double points = reponse.getPointsObtenus();
                    if (points != null) {
                        pointsObtenus += points;
                        reponse.setPointsObtenus(points);
                    } else {
                        reponse.setPointsObtenus(0);
                    }
                }
            }
        }

        return totalPoints > 0 ? (pointsObtenus / totalPoints) * 20 : 0;
    } }