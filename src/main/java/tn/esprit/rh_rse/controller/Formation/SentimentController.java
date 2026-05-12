package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.Formation.SentimentMLRequest;
import tn.esprit.rh_rse.dto.Formation.SentimentMLResponse;
import tn.esprit.rh_rse.entity.Formation.Avis;
import tn.esprit.rh_rse.service.Formation.AvisFormationService;
import tn.esprit.rh_rse.service.Formation.MLPredictionService;
import tn.esprit.rh_rse.service.Formation.SentimentAnalysisService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SentimentController {

    private final MLPredictionService mlPredictionService;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final AvisFormationService avisFormationService;

    // ========== ANALYSE DE TEXTE ==========

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeSentiment(@RequestBody SentimentMLRequest request) {
        log.info("📝 Analyse sentiment: {}", request.getText());

        Avis avis = Avis.builder()
                .commentaire(request.getText())
                .note(request.getNoteUtilisateur())
                .build();

        var result = sentimentAnalysisService.analyserSentiment(avis);

        Map<String, Object> response = new HashMap<>();
        response.put("emotion", result.getSentiment());
        response.put("sentimentScore", result.getSentimentScore());
        response.put("suggestedRating", result.getSuggestedRating());
        response.put("confidence", result.getConfiance());
        response.put("transcript", request.getText());
        response.put("detectedWords", result.getLexicalAnalysis() != null ?
                result.getLexicalAnalysis().getMotsPositifs() : new ArrayList<>());

        log.info("🎯 Résultat: {} ({}/5)", result.getSentiment(), result.getSuggestedRating());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze-ml")
    public ResponseEntity<SentimentMLResponse> analyzeWithML(@RequestBody SentimentMLRequest request) {
        log.info("📊 Analyse ML: {}", request.getText());
        SentimentMLResponse response = mlPredictionService.predictSentiment(request.getText());
        return ResponseEntity.ok(response);
    }

    // ========== GESTION DES AVIS ==========

    @GetMapping("/avis/formation/{formationId}")
    public ResponseEntity<List<Map<String, Object>>> getAvisByFormation(@PathVariable String formationId) {
        log.info("📋 Récupération des avis pour formation: {}", formationId);

        List<Avis> avisList = avisFormationService.getAvisByFormation(formationId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Avis avis : avisList) {
            var sentiment = sentimentAnalysisService.analyserSentiment(avis);

            Map<String, Object> avisData = new HashMap<>();
            avisData.put("id", avis.getId());
            avisData.put("note", avis.getNote());
            avisData.put("commentaire", avis.getCommentaire());
            avisData.put("titre", avis.getTitre());
            avisData.put("employeNom", avis.getEmployeNom());
            avisData.put("employePrenom", avis.getEmployePrenom());
            avisData.put("createdAt", avis.getCreatedAt());
            avisData.put("valide", avis.getValide());
            avisData.put("sentiment", sentiment.getSentiment());
            avisData.put("sentimentScore", sentiment.getSentimentScore());

            result.add(avisData);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/avis/{avisId}")
    public ResponseEntity<Map<String, Object>> getAvisById(@PathVariable String avisId) {
        log.info("📋 Récupération de l'avis: {}", avisId);

        Avis avis = avisFormationService.getAvisById(avisId);
        if (avis == null) {
            return ResponseEntity.notFound().build();
        }

        var sentiment = sentimentAnalysisService.analyserSentiment(avis);

        Map<String, Object> response = new HashMap<>();
        response.put("id", avis.getId());
        response.put("formationId", avis.getFormationId());
        response.put("note", avis.getNote());
        response.put("commentaire", avis.getCommentaire());
        response.put("titre", avis.getTitre());
        response.put("employeNom", avis.getEmployeNom());
        response.put("employePrenom", avis.getEmployePrenom());
        response.put("createdAt", avis.getCreatedAt());
        response.put("valide", avis.getValide());
        response.put("sentiment", sentiment.getSentiment());
        response.put("sentimentScore", sentiment.getSentimentScore());
        response.put("suggestedRating", sentiment.getSuggestedRating());
        response.put("confidence", sentiment.getConfiance());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/avis/correction/{avisId}")
    public ResponseEntity<Map<String, Object>> correctAvisNote(
            @PathVariable String avisId,
            @RequestBody Map<String, Integer> correction) {

        Integer correctedRating = correction.get("correctedRating");
        log.info("✏️ Correction avis {}: nouvelle note = {}", avisId, correctedRating);

        try {
            Avis updatedAvis = avisFormationService.updateAvisNote(avisId, correctedRating);
            var sentiment = sentimentAnalysisService.analyserSentiment(updatedAvis);

            Map<String, Object> response = new HashMap<>();
            response.put("avisId", avisId);
            response.put("ancienneNote", updatedAvis.getNote());
            response.put("nouvelleNote", correctedRating);
            response.put("nouveauSentiment", sentiment.getSentiment());
            response.put("nouveauScore", sentiment.getSentimentScore());
            response.put("message", "Correction enregistrée, le modèle va s'améliorer !");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ========== ANALYSE GLOBALE ==========

    @GetMapping("/anomalies")
    public ResponseEntity<Map<String, Object>> getAnomalies() {
        log.info("🔍 Récupération des anomalies de sentiment");

        List<Avis> allAvis = avisFormationService.getAllAvis();
        List<Map<String, Object>> anomalies = new ArrayList<>();

        for (Avis avis : allAvis) {
            var result = sentimentAnalysisService.analyserSentiment(avis);

            // Détection d'anomalie: note utilisateur vs sentiment calculé
            boolean isAnomaly = false;
            if (avis.getNote() != null) {
                int sentimentRating = result.getSuggestedRating();
                int userRating = avis.getNote();
                isAnomaly = Math.abs(sentimentRating - userRating) >= 2;
            }

            if (isAnomaly) {
                Map<String, Object> anomaly = new HashMap<>();
                anomaly.put("avisId", avis.getId());
                anomaly.put("formationId", avis.getFormationId());
                anomaly.put("commentaire", avis.getCommentaire());
                anomaly.put("noteUtilisateur", avis.getNote());
                anomaly.put("sentimentCalcule", result.getSentiment());
                anomaly.put("sentimentScore", result.getSentimentScore());
                anomaly.put("suggestedRating", result.getSuggestedRating());
                anomaly.put("confiance", result.getConfiance());
                anomalies.add(anomaly);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("anomalies", anomalies);
        response.put("totalAnomalies", anomalies.size());
        response.put("totalAvis", allAvis.size());

        log.info("📊 {} anomalies détectées sur {} avis", anomalies.size(), allAvis.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/formation/{formationId}")
    public ResponseEntity<Map<String, Object>> getFormationSentimentAnalysis(@PathVariable String formationId) {
        log.info("📊 Analyse globale des sentiments pour formation: {}", formationId);

        List<Avis> avisList = avisFormationService.getAvisByFormation(formationId);

        if (avisList.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("formationId", formationId);
            response.put("message", "Aucun avis pour cette formation");
            response.put("totalAvis", 0);
            return ResponseEntity.ok(response);
        }

        var globalAnalysis = sentimentAnalysisService.analyserAvisGroupes(avisList);

        // Calcul des statistiques supplémentaires
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        double sentimentSum = 0;

        for (Avis avis : avisList) {
            if (avis.getNote() != null) {
                ratingDistribution.put(avis.getNote(),
                        ratingDistribution.getOrDefault(avis.getNote(), 0L) + 1);
            }
            var result = sentimentAnalysisService.analyserSentiment(avis);
            sentimentSum += result.getSentimentScore();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("formationId", formationId);
        response.put("totalAvis", globalAnalysis.getTotalAvis());
        response.put("scoreMoyen", globalAnalysis.getScoreMoyen());
        response.put("sentimentGlobal", globalAnalysis.getSentimentGlobal());
        response.put("sentimentDistribution", globalAnalysis.getSentimentDistribution());
        response.put("ratingDistribution", ratingDistribution);
        response.put("tendances", globalAnalysis.getTendances());
        response.put("suggestions", globalAnalysis.getSuggestions());
        response.put("sentimentMoyen", avisList.isEmpty() ? 0 : sentimentSum / avisList.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> getGlobalStatistics() {
        log.info("📊 Récupération des statistiques globales");

        List<Avis> allAvis = avisFormationService.getAllAvis();

        if (allAvis.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Aucun avis disponible"));
        }

        var globalAnalysis = sentimentAnalysisService.analyserAvisGroupes(allAvis);

        // Distribution des notes
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        Map<String, Long> sentimentDistribution = new HashMap<>();
        Map<String, Double> moyenneParSentiment = new HashMap<>();

        Map<String, List<Double>> sentimentScores = new HashMap<>();

        for (Avis avis : allAvis) {
            if (avis.getNote() != null) {
                ratingDistribution.put(avis.getNote(),
                        ratingDistribution.getOrDefault(avis.getNote(), 0L) + 1);
            }

            var result = sentimentAnalysisService.analyserSentiment(avis);
            String sentiment = result.getSentiment();
            sentimentDistribution.put(sentiment, sentimentDistribution.getOrDefault(sentiment, 0L) + 1);

            sentimentScores.computeIfAbsent(sentiment, k -> new ArrayList<>())
                    .add(result.getSentimentScore());
        }

        // Calcul des moyennes par sentiment
        for (Map.Entry<String, List<Double>> entry : sentimentScores.entrySet()) {
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            moyenneParSentiment.put(entry.getKey(), Math.round(avg * 100.0) / 100.0);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalAvis", allAvis.size());
        response.put("scoreMoyenGlobal", globalAnalysis.getScoreMoyen());
        response.put("sentimentGlobal", globalAnalysis.getSentimentGlobal());
        response.put("ratingDistribution", ratingDistribution);
        response.put("sentimentDistribution", sentimentDistribution);
        response.put("moyenneParSentiment", moyenneParSentiment);
        response.put("tendances", globalAnalysis.getTendances());
        response.put("suggestions", globalAnalysis.getSuggestions());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tendances")
    public ResponseEntity<Map<String, Object>> getTendances() {
        log.info("📈 Récupération des tendances");

        List<Avis> allAvis = avisFormationService.getAllAvis();

        Map<String, Object> tendances = new HashMap<>();

        // Avis récents (30 derniers jours)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentAvis = allAvis.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

        // Évolution des sentiments
        Map<String, Long> sentimentsRecents = new HashMap<>();
        Map<String, Long> sentimentsAnciens = new HashMap<>();

        for (Avis avis : allAvis) {
            var result = sentimentAnalysisService.analyserSentiment(avis);
            String sentiment = result.getSentiment();

            if (avis.getCreatedAt() != null && avis.getCreatedAt().isAfter(thirtyDaysAgo)) {
                sentimentsRecents.put(sentiment, sentimentsRecents.getOrDefault(sentiment, 0L) + 1);
            } else {
                sentimentsAnciens.put(sentiment, sentimentsAnciens.getOrDefault(sentiment, 0L) + 1);
            }
        }

        tendances.put("totalAvis", allAvis.size());
        tendances.put("avisRecents", recentAvis);
        tendances.put("sentimentsRecents", sentimentsRecents);
        tendances.put("sentimentsAnciens", sentimentsAnciens);
        tendances.put("period", "30 derniers jours");

        return ResponseEntity.ok(tendances);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("ml_loaded", true);
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}