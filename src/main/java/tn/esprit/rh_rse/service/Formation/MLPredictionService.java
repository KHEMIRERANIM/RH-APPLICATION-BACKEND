package tn.esprit.rh_rse.service.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.controller.Formation.MLModelConfig;
import tn.esprit.rh_rse.dto.Formation.SentimentMLResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class MLPredictionService {

    @Autowired
    private final MLModelConfig mlModelConfig;

    public SentimentMLResponse predictSentiment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return createNeutralResponse(text);
        }

        try {
            // Prédiction avec le modèle local
            double[] probabilities = mlModelConfig.predict(text);

            // Trouver la classe avec la plus haute probabilité
            int predictedClass = argMax(probabilities);
            double confidence = probabilities[predictedClass];

            // Récupérer l'émotion et la note
            String emotion = mlModelConfig.getEmotionMap().get(predictedClass);
            int rating = mlModelConfig.getRatingMap().get(predictedClass);

            // Calculer le score de sentiment entre -1 et 1
            double sentimentScore = (predictedClass - 2) / 2.0;

            // Log pour déboguer
            log.info("🔮 Prédiction ML: '{}' → {} ({}/5) confiance={:.2f}",
                    text, emotion, rating, confidence);

            return SentimentMLResponse.builder()
                    .emotion(emotion)
                    .sentimentScore(sentimentScore)
                    .suggestedRating(rating)
                    .confidence(confidence)
                    .transcript(text)
                    .classProbabilities(probabilities)
                    .build();

        } catch (Exception e) {
            log.error("Erreur prédiction ML: {}", e.getMessage());
            return createNeutralResponse(text);
        }
    }

    private int argMax(double[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private SentimentMLResponse createNeutralResponse(String text) {
        return SentimentMLResponse.builder()
                .emotion("NEUTRE")
                .sentimentScore(0.0)
                .suggestedRating(3)
                .confidence(0.5)
                .transcript(text)
                .classProbabilities(new double[]{0, 0, 1, 0, 0})
                .build();
    }
}