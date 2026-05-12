package tn.esprit.rh_rse.service.Formation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Formation.Avis;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SentimentAnalysisService {

    @Autowired
    private MLPredictionService mlPredictionService;

    // ✅ Méthode pour analyser un groupe d'avis
    public GlobalSentimentAnalysis analyserAvisGroupes(List<Avis> avisList) {
        if (avisList == null || avisList.isEmpty()) {
            return GlobalSentimentAnalysis.builder()
                    .totalAvis(0)
                    .scoreMoyen(0.0)
                    .sentimentGlobal("NEUTRE")
                    .sentimentDistribution(new HashMap<>())
                    .tendances(new HashMap<>())
                    .suggestions(new ArrayList<>())
                    .avisAnalysees(new ArrayList<>())
                    .build();
        }

        List<SentimentResult> results = avisList.stream()
                .map(this::analyserSentiment)
                .collect(Collectors.toList());

        double scoreMoyen = results.stream()
                .mapToDouble(SentimentResult::getSentimentScore)
                .average()
                .orElse(0);

        Map<String, Long> sentimentDistribution = results.stream()
                .collect(Collectors.groupingBy(SentimentResult::getSentiment, Collectors.counting()));

        Map<String, Double> tendances = new HashMap<>();
        tendances.put("POSITIF", (double) (
                sentimentDistribution.getOrDefault("EXCELLENT", 0L) +
                        sentimentDistribution.getOrDefault("JOIE", 0L)));
        tendances.put("NEUTRE", (double) sentimentDistribution.getOrDefault("NEUTRE", 0L));
        tendances.put("NEGATIF", (double) (
                sentimentDistribution.getOrDefault("TRISTESSE", 0L) +
                        sentimentDistribution.getOrDefault("COLERE", 0L)));

        Set<String> suggestions = new LinkedHashSet<>();
        for (SentimentResult result : results) {
            if (result.getRecommendations() != null) {
                suggestions.addAll(result.getRecommendations());
            }
        }

        String sentimentGlobal;
        if (scoreMoyen >= 0.6) sentimentGlobal = "EXCELLENT";
        else if (scoreMoyen >= 0.2) sentimentGlobal = "JOIE";
        else if (scoreMoyen > -0.2) sentimentGlobal = "NEUTRE";
        else if (scoreMoyen > -0.6) sentimentGlobal = "TRISTESSE";
        else sentimentGlobal = "COLERE";

        return GlobalSentimentAnalysis.builder()
                .totalAvis(avisList.size())
                .scoreMoyen(Math.round(scoreMoyen * 100.0) / 100.0)
                .sentimentGlobal(sentimentGlobal)
                .sentimentDistribution(sentimentDistribution)
                .tendances(tendances)
                .suggestions(new ArrayList<>(suggestions))
                .avisAnalysees(results)
                .build();
    }

    // ✅ Méthode pour analyser un seul avis
    public SentimentResult analyserSentiment(Avis avis) {
        String texte = (avis.getTitre() != null ? avis.getTitre() : "") + " " +
                (avis.getCommentaire() != null ? avis.getCommentaire() : "");
        texte = texte.toLowerCase().trim();

        log.info("🔍 Analyse du texte: '{}'", texte);

        // Utiliser le modèle ML si disponible
        var mlResult = mlPredictionService.predictSentiment(texte);

        // Déterminer l'émotion et la note
        String emotion;
        int suggestedRating;
        double sentimentScore = mlResult.getSentimentScore();

        if (sentimentScore >= 0.7) {
            emotion = "EXCELLENT";
            suggestedRating = 5;
        } else if (sentimentScore >= 0.2) {
            emotion = "JOIE";
            suggestedRating = 4;
        } else if (sentimentScore > -0.2) {
            emotion = "NEUTRE";
            suggestedRating = 3;
        } else if (sentimentScore > -0.7) {
            emotion = "TRISTESSE";
            suggestedRating = 2;
        } else {
            emotion = "COLERE";
            suggestedRating = 1;
        }

        // Si une note utilisateur existe, combiner
        if (avis.getNote() != null) {
            double noteScore = (avis.getNote() - 3) / 2.0;
            sentimentScore = sentimentScore * 0.6 + noteScore * 0.4;

            if (sentimentScore >= 0.7) emotion = "EXCELLENT";
            else if (sentimentScore >= 0.2) emotion = "JOIE";
            else if (sentimentScore > -0.2) emotion = "NEUTRE";
            else if (sentimentScore > -0.7) emotion = "TRISTESSE";
            else emotion = "COLERE";
        }

        return SentimentResult.builder()
                .avisId(avis.getId())
                .sentimentScore(sentimentScore)
                .sentiment(emotion)
                .suggestedRating(suggestedRating)
                .confiance(mlResult.getConfidence())
                .anomaly(detecterAnomalie(avis, sentimentScore, avis.getNote() != null ? (avis.getNote() - 3) / 2.0 : 0))
                .recommendations(genererRecommandations(texte, sentimentScore))
                .build();
    }

    private boolean detecterAnomalie(Avis avis, double sentimentScore, double noteScore) {
        if (avis.getNote() == null) return false;
        return Math.abs(sentimentScore - noteScore) > 0.5;
    }

    private List<String> genererRecommandations(String texte, double sentimentScore) {
        List<String> recommendations = new ArrayList<>();
        if (sentimentScore < -0.3) {
            recommendations.add("🚨 Avis négatif détecté - Une attention particulière est nécessaire");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Continuer à maintenir la qualité de la formation");
        }
        return recommendations;
    }
}