package tn.esprit.rh_rse.service.Formation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rh_rse.entity.Formation.*;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.repository.Formation.ExamenRepository;
import tn.esprit.rh_rse.repository.Formation.ParticipantInscriptionRepository;
import tn.esprit.rh_rse.repository.Formation.ResultatExamenRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final ResultatExamenRepository resultatRepository;
    private final ParticipantInscriptionRepository participantRepository;
    private final UserRepository userRepository;
    private final CertificationService certificationService;
    private final PointFormationService pointFormationService;
    private final FraudService fraudService;  // ✅ NOUVEAU - Injection anti-fraude

    private static final double SEUIL_REUSSITE = 10.0;

    @Autowired(required = false)
    private GeminiService geminiService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== GESTION DES EXAMENS ====================

    public List<Examen> getExamensByFormation(String formationId) {
        log.info("📋 Récupération des examens pour formation: {}", formationId);
        return examenRepository.findByFormationId(formationId);
    }

    public Examen getExamenById(String examenId) {
        return examenRepository.findById(examenId)
                .orElseThrow(() -> new RuntimeException("Examen non trouvé: " + examenId));
    }

    @Transactional
    public Examen createExamen(Examen examen) {
        log.info("📝 Création examen: {}", examen.getTitre());

        if (examen.getQuestions() != null) {
            for (Question question : examen.getQuestions()) {
                if (question.getId() == null) {
                    question.setId(UUID.randomUUID().toString());
                }
                if (question.getOptions() != null) {
                    for (OptionQuestion option : question.getOptions()) {
                        if (option.getId() == null) {
                            option.setId(UUID.randomUUID().toString());
                        }
                    }
                }
            }
        }

        examen.setCreatedAt(LocalDateTime.now());
        examen.setUpdatedAt(LocalDateTime.now());

        return examenRepository.save(examen);
    }

    @Transactional
    public Examen updateExamen(String examenId, Examen examenDetails) {
        log.info("✏️ Mise à jour examen: {}", examenId);

        Examen examen = getExamenById(examenId);
        examen.setTitre(examenDetails.getTitre());
        examen.setDescription(examenDetails.getDescription());
        examen.setDureeMinutes(examenDetails.getDureeMinutes());
        examen.setDateLimite(examenDetails.getDateLimite());
        examen.setQuestions(examenDetails.getQuestions());
        examen.setUpdatedAt(LocalDateTime.now());

        return examenRepository.save(examen);
    }

    @Transactional
    public void deleteExamen(String examenId) {
        log.info("🗑️ Suppression examen: {}", examenId);
        resultatRepository.deleteByExamenId(examenId);
        examenRepository.deleteById(examenId);
    }

    // ==================== VÉRIFICATION ACCÈS EXAMEN (MODIFIÉ AVEC FRAUDE) ====================

    public boolean canAccessExamen(String formationId, String employeId) {
        log.info("🔍 Vérification accès examen - Formation: {}, Employé: {}", formationId, employeId);

        // ✅ Vérifier si l'étudiant est bloqué pour fraude
        List<Examen> examens = examenRepository.findByFormationId(formationId);
        for (Examen examen : examens) {
            if (fraudService.isStudentBlocked(examen.getId(), employeId)) {
                log.warn("❌ Accès REFUSÉ - Employé {} est BLOQUÉ pour fraude", employeId);
                return false;
            }
        }

        Optional<ParticipantInscription> inscriptionOpt = participantRepository.findByFormationIdAndEmployeId(formationId, employeId);

        if (inscriptionOpt.isEmpty()) {
            log.warn("❌ Employé non inscrit à la formation {}", formationId);
            return false;
        }

        ParticipantInscription inscription = inscriptionOpt.get();
        log.info("📋 Inscription trouvée - Statut: {}, Présence validée: {}",
                inscription.getStatut(), inscription.getPresenceValidee());

        if (!inscription.getPresenceValidee()) {
            log.warn("❌ Présence non validée pour l'employé {}", employeId);
            return false;
        }

        for (Examen examen : examens) {
            if (resultatRepository.existsByExamenIdAndEmployeId(examen.getId(), employeId)) {
                log.warn("⚠️ Employé a déjà passé l'examen {}", examen.getId());
                return false;
            }
        }

        log.info("✅ Accès examen autorisé pour l'employé {}", employeId);
        return true;
    }

    public Map<String, Object> getExamenStatus(String formationId, String employeId) {
        Map<String, Object> status = new HashMap<>();

        Optional<ParticipantInscription> inscriptionOpt = participantRepository.findByFormationIdAndEmployeId(formationId, employeId);

        if (inscriptionOpt.isEmpty()) {
            status.put("accessible", false);
            status.put("message", "Vous n'êtes pas inscrit à cette formation");
            return status;
        }

        ParticipantInscription inscription = inscriptionOpt.get();
        boolean presenceValidee = inscription.getPresenceValidee();
        status.put("presenceValidee", presenceValidee);

        List<Examen> examens = examenRepository.findByFormationId(formationId);

        // ✅ Vérifier si bloqué pour fraude
        for (Examen examen : examens) {
            if (fraudService.isStudentBlocked(examen.getId(), employeId)) {
                status.put("accessible", false);
                status.put("bloquePourFraude", true);
                status.put("message", "❌ Vous êtes bloqué pour avoir quitté la page examen à plusieurs reprises. Contactez votre formateur.");
                return status;
            }
        }

        for (Examen examen : examens) {
            var resultat = resultatRepository.findByExamenIdAndEmployeId(examen.getId(), employeId);
            if (resultat.isPresent()) {
                status.put("dejaPasse", true);
                status.put("note", resultat.get().getNote());
                status.put("certifie", resultat.get().getNote() >= SEUIL_REUSSITE);
                status.put("message", "Vous avez déjà passé cet examen");
                return status;
            }
        }

        boolean accessible = presenceValidee;
        status.put("accessible", accessible);
        status.put("dejaPasse", false);
        status.put("bloquePourFraude", false);

        if (!accessible) {
            status.put("message", "Votre présence doit être validée pour accéder à l'examen");
        } else {
            status.put("message", "Examen disponible");
        }

        return status;
    }

    // ==================== SOUMISSION ET CORRECTION (MODIFIÉ AVEC FRAUDE) ====================

    @Transactional
    public ResultatExamen soumettreExamen(String examenId, String employeId, List<Reponse> reponses) {
        log.info("📝 Soumission examen {} par employé {}", examenId, employeId);

        // ✅ VÉRIFIER SI L'ÉTUDIANT N'EST PAS BLOQUÉ POUR FRAUDE
        if (fraudService.isStudentBlocked(examenId, employeId)) {
            long violationCount = fraudService.getViolationCount(examenId, employeId);
            throw new RuntimeException("❌ Vous êtes bloqué pour avoir quitté la page examen " + violationCount + " fois. Contactez votre formateur.");
        }

        if (resultatRepository.existsByExamenIdAndEmployeId(examenId, employeId)) {
            throw new RuntimeException("Vous avez déjà soumis cet examen");
        }

        Examen examen = getExamenById(examenId);

        if (examen.getDateLimite() != null && LocalDateTime.now().isAfter(examen.getDateLimite())) {
            throw new RuntimeException("Date limite dépassée pour cet examen");
        }

        if (!canAccessExamen(examen.getFormationId(), employeId)) {
            throw new RuntimeException("Vous n'avez pas accès à cet examen");
        }

        User employe = userRepository.findById(employeId)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé"));

        if (reponses == null) reponses = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        CorrectionResult correction = calculerNoteEtCorrectionAvecFeedback(examen, reponses);

        long tempsPrise = System.currentTimeMillis() - startTime;

        // Construire le JSON des détails de correction
        Map<String, Object> detailsCorrection = new HashMap<>();
        for (Reponse reponse : correction.reponsesCorrigees) {
            Map<String, Object> questionDetail = new HashMap<>();
            questionDetail.put("pointsObtenus", reponse.getPointsObtenus());
            questionDetail.put("feedback", reponse.getFeedback());
            questionDetail.put("commentaireIa", reponse.getCommentaireIa());
            detailsCorrection.put(reponse.getQuestionId(), questionDetail);
        }

        String correctionDetailleeJson = "";
        try {
            correctionDetailleeJson = objectMapper.writeValueAsString(detailsCorrection);
        } catch (Exception e) {
            log.error("Erreur sérialisation correction détaillée", e);
        }

        // ✅ Ajouter le nombre de violations de fraude au résultat
        long fraudViolations = fraudService.getViolationCount(examenId, employeId);

        ResultatExamen resultat = ResultatExamen.builder()
                .examenId(examenId)
                .employeId(employeId)
                .employeNom(employe.getNom())
                .employePrenom(employe.getPrenom())
                .employeEmail(employe.getEmail())
                .note(correction.note)
                .reponses(correction.reponsesCorrigees)
                .submittedAt(LocalDateTime.now())
                .valide(correction.note >= SEUIL_REUSSITE)
                .feedbackIA(construireFeedbackGlobal(correction))
                .correctionDetaillee(correctionDetailleeJson)
                .tempsPriseMs(tempsPrise)
                .iaUtilisee(geminiService != null)
                .build();

        ResultatExamen savedResultat = resultatRepository.save(resultat);

        log.info("✅ Examen soumis - Note: {}/20, Violations fraude: {}", correction.note, fraudViolations);

        // Bonus pour note > 15
        if (correction.note > 15) {
            String formationTitre = examen.getTitre();
            String raison = String.format("🎉 BONUS EXCELLENCE - Note %.1f/20 à l'examen '%s'", correction.note, formationTitre);

            try {
                pointFormationService.ajouterPoints(employeId, 500, raison);
                log.info("🎁 +500 points attribués à {}", employe.getPrenom() + " " + employe.getNom());
            } catch (Exception e) {
                log.error("❌ Erreur lors de l'attribution du bonus: {}", e.getMessage());
            }
        }

        // ✅ Pénalité pour fraude (si violations > 0)
        if (fraudViolations > 0) {
            int penalitePoints = (int) (fraudViolations * 50);
            try {
                pointFormationService.ajouterPoints(employeId, -penalitePoints,
                        String.format("⚠️ PÉNALITÉ - %d tentatives de quitter la page examen", fraudViolations));
                log.warn("⚠️ Pénalité de {} points appliquée pour {} violations", penalitePoints, fraudViolations);
            } catch (Exception e) {
                log.error("Erreur application pénalité: {}", e.getMessage());
            }
        }

        if (resultat.getValide()) {
            certificationService.genererCertification(examen.getFormationId(), employeId, savedResultat);
        }

        return savedResultat;
    }

    private CorrectionResult calculerNoteEtCorrectionAvecFeedback(Examen examen, List<Reponse> reponses) {
        double totalPoints = 0;
        double pointsObtenus = 0;
        List<Reponse> reponsesCorrigees = new ArrayList<>();

        if (examen.getQuestions() == null || examen.getQuestions().isEmpty()) {
            CorrectionResult result = new CorrectionResult();
            result.note = 0;
            result.reponsesCorrigees = new ArrayList<>();
            return result;
        }

        for (Question question : examen.getQuestions()) {
            totalPoints += question.getPoints();

            Reponse reponse = trouverReponse(reponses, question.getId());

            if (reponse != null && reponse.getReponse() != null && !reponse.getReponse().isEmpty()) {
                EvaluationResult eval = evaluerQuestionAvecFeedback(question, reponse.getReponse());
                pointsObtenus += eval.points;

                reponse.setPointsObtenus((int) Math.round(eval.points));
                reponse.setFeedback(eval.feedback);
                reponse.setCommentaireIa(eval.commentaire);
                reponsesCorrigees.add(reponse);

                log.debug("Question: {}/{} pts - {}", eval.points, question.getPoints(), eval.feedback);
            } else {
                Reponse reponseVide = Reponse.builder()
                        .questionId(question.getId())
                        .reponse("")
                        .pointsObtenus(0)
                        .feedback("Aucune réponse fournie")
                        .build();
                reponsesCorrigees.add(reponseVide);
            }
        }

        double note = totalPoints > 0 ? (pointsObtenus * 20.0 / totalPoints) : 0;
        note = Math.round(note * 100.0) / 100.0;

        CorrectionResult result = new CorrectionResult();
        result.note = note;
        result.reponsesCorrigees = reponsesCorrigees;
        result.pointsObtenus = pointsObtenus;
        result.totalPoints = totalPoints;

        return result;
    }

    private EvaluationResult evaluerQuestionAvecFeedback(Question question, String reponseUtilisateur) {
        if (reponseUtilisateur == null || reponseUtilisateur.trim().isEmpty()) {
            return new EvaluationResult(0, "Aucune réponse", "");
        }

        switch (question.getType()) {
            case "QCM":
                int points = evaluerQCM(question, reponseUtilisateur);
                return new EvaluationResult(points,
                        points > 0 ? "✅ Bonne réponse !" : "❌ Mauvaise réponse",
                        "");

            case "TEXTE":
                if (geminiService != null && question.getCorrectAnswer() != null) {
                    GeminiService.CorrectionIA iaCorrection = geminiService.evaluerReponseDeveloppement(
                            question.getTexte(),
                            question.getCorrectAnswer(),
                            reponseUtilisateur,
                            "TEXTE",
                            question.getPoints()
                    );
                    return new EvaluationResult(iaCorrection.getPointsObtenus(),
                            iaCorrection.getFeedback(),
                            iaCorrection.getCommentaire());
                }
                int pointsTexte = evaluerTexte(question, reponseUtilisateur);
                return new EvaluationResult(pointsTexte,
                        pointsTexte == question.getPoints() ? "✅ Réponse correcte" : "⚠️ Réponse partiellement correcte",
                        "");

            case "CODE":
                if (geminiService != null) {
                    String codeAttendu = question.getCodeTemplate() != null ? question.getCodeTemplate() : "";
                    GeminiService.CorrectionIA iaCorrection = geminiService.evaluerReponseDeveloppement(
                            question.getTexte(),
                            codeAttendu,
                            reponseUtilisateur,
                            "CODE",
                            question.getPoints()
                    );
                    return new EvaluationResult(iaCorrection.getPointsObtenus(),
                            iaCorrection.getFeedback(),
                            iaCorrection.getCommentaire());
                }
                int pointsCode = evaluerCode(question, reponseUtilisateur);
                return new EvaluationResult(pointsCode,
                        pointsCode == question.getPoints() ? "✅ Code correct" : "⚠️ Code partiellement correct",
                        "");

            default:
                return new EvaluationResult(0, "Type de question non supporté", "");
        }
    }

    private String construireFeedbackGlobal(CorrectionResult correction) {
        if (correction.reponsesCorrigees.isEmpty()) {
            return "Aucune réponse fournie. Veuillez contacter votre formateur.";
        }

        long reussies = correction.reponsesCorrigees.stream()
                .filter(r -> r.getPointsObtenus() > 0)
                .count();

        double pourcentage = (correction.pointsObtenus * 100.0 / correction.totalPoints);

        if (pourcentage >= 75) {
            return String.format("🎉 Excellente performance ! %.0f%% de réussite (%d/%d points). %d questions réussies sur %d. Continuez ainsi !",
                    pourcentage, (int)correction.pointsObtenus, (int)correction.totalPoints, reussies, correction.reponsesCorrigees.size());
        } else if (pourcentage >= 50) {
            return String.format("👍 Bon travail ! %.0f%% de réussite (%d/%d points). %d questions réussies sur %d. Quelques points à améliorer.",
                    pourcentage, (int)correction.pointsObtenus, (int)correction.totalPoints, reussies, correction.reponsesCorrigees.size());
        } else {
            return String.format("📚 À réviser. %.0f%% de réussite (%d/%d points). %d questions réussies sur %d. N'hésitez pas à reprendre la formation.",
                    pourcentage, (int)correction.pointsObtenus, (int)correction.totalPoints, reussies, correction.reponsesCorrigees.size());
        }
    }

    private Reponse trouverReponse(List<Reponse> reponses, String questionId) {
        if (reponses == null) return null;
        return reponses.stream()
                .filter(r -> r.getQuestionId() != null && r.getQuestionId().equals(questionId))
                .findFirst()
                .orElse(null);
    }

    private double evaluerQuestion(Question question, String reponseUtilisateur) {
        if (reponseUtilisateur == null || reponseUtilisateur.trim().isEmpty()) {
            return 0;
        }

        switch (question.getType()) {
            case "QCM":
                return evaluerQCM(question, reponseUtilisateur);
            case "TEXTE":
                if (geminiService != null && question.getCorrectAnswer() != null) {
                    GeminiService.CorrectionIA correction = geminiService.evaluerReponseDeveloppement(
                            question.getTexte(),
                            question.getCorrectAnswer(),
                            reponseUtilisateur,
                            "TEXTE",
                            question.getPoints()
                    );
                    return correction.getPointsObtenus();
                }
                return evaluerTexte(question, reponseUtilisateur);
            case "CODE":
                if (geminiService != null) {
                    String codeAttendu = question.getCodeTemplate() != null ? question.getCodeTemplate() : "";
                    GeminiService.CorrectionIA correction = geminiService.evaluerReponseDeveloppement(
                            question.getTexte(),
                            codeAttendu,
                            reponseUtilisateur,
                            "CODE",
                            question.getPoints()
                    );
                    return correction.getPointsObtenus();
                }
                return evaluerCode(question, reponseUtilisateur);
            default:
                return 0;
        }
    }

    private int evaluerQCM(Question question, String reponseUtilisateur) {
        if (question.getOptions() == null) return 0;
        for (OptionQuestion option : question.getOptions()) {
            if (option.getId().equals(reponseUtilisateur) && Boolean.TRUE.equals(option.getEstCorrect())) {
                return question.getPoints();
            }
        }
        return 0;
    }

    private int evaluerTexte(Question question, String reponseUtilisateur) {
        if (question.getCorrectAnswer() == null) return 0;
        String reponseNorm = reponseUtilisateur.trim().toLowerCase();
        String correctNorm = question.getCorrectAnswer().trim().toLowerCase();
        if (reponseNorm.equals(correctNorm)) {
            return question.getPoints();
        }
        if (reponseNorm.contains(correctNorm) || correctNorm.contains(reponseNorm)) {
            return question.getPoints() / 2;
        }
        return 0;
    }

    private int evaluerCode(Question question, String reponseUtilisateur) {
        if (reponseUtilisateur == null || reponseUtilisateur.trim().isEmpty()) {
            return 0;
        }
        if (question.getCodeTemplate() != null && !question.getCodeTemplate().isEmpty()) {
            String[] keywords = question.getCodeTemplate().toLowerCase().split("\\s+");
            int found = 0;
            for (String keyword : keywords) {
                if (reponseUtilisateur.toLowerCase().contains(keyword)) {
                    found++;
                }
            }
            if (keywords.length > 0) {
                return (int) Math.round((double) question.getPoints() * found / keywords.length);
            }
        }
        return reponseUtilisateur.length() > 50 ? question.getPoints() : question.getPoints() / 2;
    }

    // ==================== RÉSULTATS ====================

    public List<ResultatExamen> getResultatsByExamen(String examenId) {
        log.info("📊 Récupération des résultats pour examen: {}", examenId);
        return resultatRepository.findByExamenIdOrderByNoteDesc(examenId);
    }

    public ResultatExamen getResultatByEmploye(String examenId, String employeId) {
        return resultatRepository.findByExamenIdAndEmployeId(examenId, employeId)
                .orElse(null);
    }

    public List<ResultatExamen> getResultatsByEmploye(String employeId) {
        return resultatRepository.findByEmployeId(employeId);
    }

    public Map<String, Object> getStatistiquesExamen(String examenId) {
        Map<String, Object> stats = new HashMap<>();
        List<ResultatExamen> resultats = resultatRepository.findByExamenId(examenId);

        if (resultats.isEmpty()) {
            stats.put("totalSoumis", 0);
            stats.put("moyenne", 0);
            stats.put("min", 0);
            stats.put("max", 0);
            stats.put("tauxReussite", 0);
            stats.put("tauxEchec", 0);
            return stats;
        }

        double moyenne = resultats.stream().mapToDouble(ResultatExamen::getNote).average().orElse(0);
        double min = resultats.stream().mapToDouble(ResultatExamen::getNote).min().orElse(0);
        double max = resultats.stream().mapToDouble(ResultatExamen::getNote).max().orElse(0);
        long reussis = resultats.stream().filter(r -> r.getNote() >= SEUIL_REUSSITE).count();
        long echoues = resultats.size() - reussis;

        stats.put("totalSoumis", resultats.size());
        stats.put("moyenne", Math.round(moyenne * 100.0) / 100.0);
        stats.put("min", Math.round(min * 100.0) / 100.0);
        stats.put("max", Math.round(max * 100.0) / 100.0);
        stats.put("tauxReussite", (reussis * 100.0 / resultats.size()));
        stats.put("tauxEchec", (echoues * 100.0 / resultats.size()));

        return stats;
    }

    // ==================== CLASSES INTERNES ====================

    private static class EvaluationResult {
        double points;
        String feedback;
        String commentaire;

        EvaluationResult(double points, String feedback, String commentaire) {
            this.points = points;
            this.feedback = feedback;
            this.commentaire = commentaire;
        }
    }

    private static class CorrectionResult {
        double note;
        List<Reponse> reponsesCorrigees;
        double pointsObtenus;
        double totalPoints;
    }
}