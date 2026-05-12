package tn.esprit.rh_rse.controller.Formation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Slf4j
public class MLModelConfig {

    @Value("${ml.model.json.path:se2/sentiment_model_for_java.json}")
    private String modelJsonPath;

    private Map<String, Integer> vocabulary;
    private double[][] featureLogProb;
    private double[] classPrior;
    private Map<Integer, String> emotionMap;
    private Map<Integer, Integer> ratingMap;

    @PostConstruct
    public void init() {
        log.info("🚀 Chargement du modèle ML...");
        try {
            loadModelFromJson();
            log.info("✅ Modèle ML chargé avec succès!");
            log.info("   - Taille du vocabulaire: {}", vocabulary.size());
            log.info("   - Classes: {}", emotionMap);
        } catch (Exception e) {
            log.error("❌ Erreur chargement modèle ML: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadModelFromJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClassPathResource resource = new ClassPathResource(modelJsonPath);

        Map<String, Object> jsonData = mapper.readValue(resource.getInputStream(), Map.class);

        // ✅ 1. Charger le vocabulaire (String -> Integer)
        Map<String, Integer> rawVocab = (Map<String, Integer>) jsonData.get("vocabulary");
        vocabulary = new ConcurrentHashMap<>();
        for (Map.Entry<String, Integer> entry : rawVocab.entrySet()) {
            vocabulary.put(entry.getKey().toLowerCase(), entry.getValue());
        }

        // ✅ 2. Charger les probabilités des features
        List<List<Double>> rawFeatureLogProb = (List<List<Double>>) jsonData.get("feature_log_prob");
        featureLogProb = new double[rawFeatureLogProb.size()][];
        for (int i = 0; i < rawFeatureLogProb.size(); i++) {
            List<Double> row = rawFeatureLogProb.get(i);
            featureLogProb[i] = new double[row.size()];
            for (int j = 0; j < row.size(); j++) {
                featureLogProb[i][j] = row.get(j);
            }
        }

        // ✅ 3. Charger les priors des classes
        List<Double> rawClassPrior = (List<Double>) jsonData.get("class_prior");
        classPrior = new double[rawClassPrior.size()];
        for (int i = 0; i < rawClassPrior.size(); i++) {
            classPrior[i] = rawClassPrior.get(i);
        }

        // ✅ 4. Charger la map d'émotion (CORRECTION: les clés peuvent être String ou Integer)
        emotionMap = new HashMap<>();
        Object rawEmotionMap = jsonData.get("emotion_map");

        if (rawEmotionMap instanceof Map) {
            Map<?, ?> emotionMapRaw = (Map<?, ?>) rawEmotionMap;
            for (Map.Entry<?, ?> entry : emotionMapRaw.entrySet()) {
                // ✅ Convertir la clé en Integer (peut être String ou Integer)
                Integer key;
                if (entry.getKey() instanceof Integer) {
                    key = (Integer) entry.getKey();
                } else {
                    key = Integer.parseInt(entry.getKey().toString());
                }
                String value = entry.getValue().toString();
                emotionMap.put(key, value);
            }
        }

        // ✅ 5. Charger la map des ratings (CORRECTION)
        ratingMap = new HashMap<>();
        Object rawRatingMap = jsonData.get("rating_map");

        if (rawRatingMap instanceof Map) {
            Map<?, ?> ratingMapRaw = (Map<?, ?>) rawRatingMap;
            for (Map.Entry<?, ?> entry : ratingMapRaw.entrySet()) {
                // ✅ Convertir la clé en Integer
                Integer key;
                if (entry.getKey() instanceof Integer) {
                    key = (Integer) entry.getKey();
                } else {
                    key = Integer.parseInt(entry.getKey().toString());
                }
                Integer value;
                if (entry.getValue() instanceof Integer) {
                    value = (Integer) entry.getValue();
                } else {
                    value = Integer.parseInt(entry.getValue().toString());
                }
                ratingMap.put(key, value);
            }
        }

        log.info("📊 Maps chargées:");
        log.info("   - emotionMap: {}", emotionMap);
        log.info("   - ratingMap: {}", ratingMap);
    }

    public double[] predict(String text) {
        try {
            // Tokenisation
            String[] words = text.toLowerCase().split("\\s+");
            Map<Integer, Integer> wordCounts = new HashMap<>();

            for (String word : words) {
                Integer index = vocabulary.get(word);
                if (index != null && index < featureLogProb[0].length) {
                    wordCounts.put(index, wordCounts.getOrDefault(index, 0) + 1);
                }
            }

            // Calcul des scores par classe (Naive Bayes)
            int numClasses = classPrior.length;
            double[] classScores = new double[numClasses];

            for (int c = 0; c < numClasses; c++) {
                classScores[c] = classPrior[c];
                for (Map.Entry<Integer, Integer> entry : wordCounts.entrySet()) {
                    int wordIdx = entry.getKey();
                    int count = entry.getValue();
                    if (wordIdx < featureLogProb[c].length) {
                        classScores[c] += featureLogProb[c][wordIdx] * count;
                    }
                }
            }

            // Softmax pour obtenir les probabilités
            double[] probabilities = softmax(classScores);
            return probabilities;

        } catch (Exception e) {
            log.error("Erreur prédiction ML: {}", e.getMessage());
            return new double[]{0, 0, 1, 0, 0};
        }
    }

    private double[] softmax(double[] scores) {
        double max = Arrays.stream(scores).max().orElse(0);
        double[] exp = new double[scores.length];
        double sum = 0;
        for (int i = 0; i < scores.length; i++) {
            exp[i] = Math.exp(scores[i] - max);
            sum += exp[i];
        }
        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sum;
        }
        return exp;
    }

    public Map<Integer, String> getEmotionMap() { return emotionMap; }
    public Map<Integer, Integer> getRatingMap() { return ratingMap; }
    public double[][] getFeatureLogProb() { return featureLogProb; }
    public double[] getClassPrior() { return classPrior; }
    public Map<String, Integer> getVocabulary() { return vocabulary; }
}