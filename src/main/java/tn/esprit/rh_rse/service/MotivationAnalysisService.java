package tn.esprit.rh_rse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.rh_rse.dto.response.AnalysisResult;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MotivationAnalysisService {

    @Value("${openrouter.api.key}")
    private String openRouterApiKey;

    private static final String[] MODELS = {
            "google/gemma-3-4b-it:free",
            "meta-llama/llama-3.2-3b-instruct:free"
    };

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";

    private static final int MAX_TOKENS = 900;
    private static final double TEMPERATURE = 0.2;
    private static final long RETRY_DELAY_MS = 3000;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisResult analyzeLetterForJob(byte[] fileContent,
                                              String fileName,
                                              String jobTitle,
                                              String jobDescription) {

        if (fileContent == null || fileContent.length == 0) {
            log.warn("Fichier vide ou null");
            return buildFallbackResult("Fichier vide", "Veuillez uploader un PDF valide.");
        }

        String letterText = extractTextFromPdf(fileContent, fileName);
        if (letterText.isBlank()) {
            log.warn("Aucun texte extrait depuis le PDF [{}]", fileName);
            return buildFallbackResult(
                    "Aucune lettre détectée",
                    "Le PDF semble vide, protégé ou mal formaté."
            );
        }

        log.info("Texte extrait ({} chars) pour poste '{}'", letterText.length(), jobTitle);

        String prompt = buildPrompt(letterText, jobTitle, jobDescription);
        boolean rateLimited = false;

        for (int i = 0; i < MODELS.length; i++) {
            String model = MODELS[i];
            log.info("Tentative avec modèle: {}", model);

            try {
                String rawResponse = callOpenRouterApi(prompt, model);

                if (rawResponse == null || rawResponse.isBlank()) {
                    log.warn("Réponse vide du modèle {}", model);
                    sleepBeforeRetry(i);
                    continue;
                }

                AnalysisResult result = parseAnalysis(rawResponse);

                if (isValidResult(result)) {
                    log.info("✅ Analyse IA réussie avec: {}", model);
                    return result;
                }

                log.warn("Réponse IA reçue mais invalide pour {}", model);
                sleepBeforeRetry(i);

            } catch (HttpClientErrorException.TooManyRequests e) {
                rateLimited = true;
                log.warn("Modèle {} temporairement limité (429)", model);
                sleepBeforeRetry(i);
            } catch (Exception e) {
                log.warn("Échec du modèle {}: {}", model, e.getMessage());
                sleepBeforeRetry(i);
            }
        }

        log.warn("Tous les modèles IA ont échoué, bascule vers analyse locale.");

        AnalysisResult localResult = analyzeLocally(letterText, jobTitle, jobDescription);

        if (rateLimited) {
            localResult.getSuggestions().add("Analyse générée en mode local car le service IA était momentanément saturé.");
        } else {
            localResult.getSuggestions().add("Analyse générée en mode local comme solution de secours.");
        }

        return localResult;
    }

    private String extractTextFromPdf(byte[] fileContent, String fileName) {
        try (PDDocument document = Loader.loadPDF(fileContent)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            log.error("Erreur extraction PDF [{}]: {}", fileName, e.getMessage(), e);
            return "";
        }
    }

    private String buildPrompt(String letter, String jobTitle, String jobDescription) {
        return String.format("""
                Tu es un expert RH senior.

                Analyse la lettre de motivation suivante pour le poste "%s".

                Description du poste :
                %s

                Lettre de motivation :
                %s

                IMPORTANT :
                - Réponds uniquement avec du JSON valide
                - Pas de markdown
                - Pas de backticks
                - Pas de texte avant ou après le JSON
                - Utilise exactement les noms de champs suivants

                Format attendu :
                {
                  "scoreGlobal": 72,
                  "scores": {
                    "pertinence": 8,
                    "clarte": 7,
                    "motivation": 6,
                    "professionnalisme": 8,
                    "originalite": 5
                  },
                  "pointsForts": ["Bonne structure", "Motivation claire"],
                  "aAmeliorer": ["Manque d'exemples concrets"],
                  "suggestions": ["Ajouter un projet lié au poste"],
                  "verdict": "BON",
                  "langue": "Français"
                }

                Règles :
                - scoreGlobal entre 0 et 100
                - chaque score entre 0 et 10
                - verdict ∈ EXCELLENT, BON, MOYEN, INSUFFISANT
                - langue = Français
                """,
                safe(jobTitle, "Poste cible"),
                safe(jobDescription, "Aucune description fournie"),
                letter
        );
    }

    @SuppressWarnings("unchecked")
    private String callOpenRouterApi(String prompt, String model) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openRouterApiKey);
            headers.set("HTTP-Referer", "http://localhost:4200");
            headers.set("X-Title", "RH-RSE Career Module");

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(message));
            body.put("max_tokens", MAX_TOKENS);
            body.put("temperature", TEMPERATURE);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    OPENROUTER_URL,
                    HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getBody() == null) {
                log.warn("OpenRouter [{}] a renvoyé un body null", model);
                return null;
            }

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.getBody().get("choices");

            if (choices == null || choices.isEmpty()) {
                log.warn("OpenRouter [{}] n'a renvoyé aucun choix", model);
                return null;
            }

            Map<String, Object> messageObj =
                    (Map<String, Object>) choices.get(0).get("message");

            if (messageObj == null) {
                log.warn("OpenRouter [{}] n'a renvoyé aucun message", model);
                return null;
            }

            String content = (String) messageObj.get("content");

            log.info("OpenRouter [{}] OK — preview: {}",
                    model,
                    content != null ? content.substring(0, Math.min(120, content.length())) : "null");

            return content;

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Rate limit OpenRouter [{}] : {}", model, e.getMessage());
            throw e;

        } catch (Exception e) {
            String msg = e.getMessage();
            log.error("Erreur OpenRouter [{}]: {}",
                    model,
                    msg != null ? msg.substring(0, Math.min(250, msg.length())) : "unknown");
            return null;
        }
    }

    private AnalysisResult parseAnalysis(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }

        try {
            String clean = cleanJson(rawJson);

            JsonNode root = objectMapper.readTree(clean);

            AnalysisResult result = new AnalysisResult();

            result.setScoreGlobal(readInt(root, "scoreGlobal", 0));

            Map<String, Integer> scores = Map.of(
                    "pertinence", readInt(root.path("scores"), "pertinence", 0),
                    "clarte", readInt(root.path("scores"), "clarte", 0),
                    "motivation", readInt(root.path("scores"), "motivation", 0),
                    "professionnalisme", readInt(root.path("scores"), "professionnalisme", 0),
                    "originalite", readInt(root.path("scores"), "originalite", 0)
            );
            result.setScores(scores);

            result.setPointsForts(readStringList(root, "pointsForts"));
            result.setAAmeliorer(new ArrayList<>(readStringListWithAliases(root, "aAmeliorer", "aameliorer")));
            result.setSuggestions(new ArrayList<>(readStringList(root, "suggestions")));
            result.setVerdict(readText(root, "verdict", "MOYEN"));
            result.setLangue(readText(root, "langue", "Français"));

            log.info("Parsing OK — scoreGlobal={}, verdict={}",
                    result.getScoreGlobal(), result.getVerdict());

            return result;

        } catch (Exception e) {
            log.error("Erreur parsing JSON: {} — raw: {}",
                    e.getMessage(),
                    rawJson.substring(0, Math.min(500, rawJson.length())));
            return null;
        }
    }

    private AnalysisResult analyzeLocally(String letterText, String jobTitle, String jobDescription) {
        String text = normalize(letterText);
        String job = normalize(safe(jobTitle, ""));
        String description = normalize(safe(jobDescription, ""));

        List<String> motivationWords = List.of(
                "motivé", "motivation", "passion", "intéressé", "enthousiaste",
                "engagé", "déterminé", "rigoureux", "sérieux", "dynamique"
        );

        List<String> professionalWords = List.of(
                "compétence", "expérience", "projet", "stage", "analyse",
                "développement", "gestion", "responsable", "collaboration", "équipe"
        );

        int length = letterText.length();
        int pertinence = 4;
        int clarte = 4;
        int motivation = 4;
        int professionnalisme = 4;
        int originalite = 4;

        List<String> pointsForts = new ArrayList<>();
        List<String> aAmeliorer = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (length >= 800) {
            clarte += 2;
            professionnalisme += 1;
            pointsForts.add("La lettre est suffisamment développée.");
        } else if (length >= 400) {
            clarte += 1;
            pointsForts.add("La lettre a une longueur correcte.");
        } else {
            aAmeliorer.add("La lettre est trop courte.");
            suggestions.add("Développer davantage les motivations et les expériences.");
        }

        long motivationCount = countMatches(text, motivationWords);
        if (motivationCount >= 3) {
            motivation += 3;
            pointsForts.add("La motivation est bien exprimée.");
        } else if (motivationCount >= 1) {
            motivation += 1;
        } else {
            aAmeliorer.add("La motivation n'est pas assez mise en avant.");
            suggestions.add("Ajouter des phrases montrant un réel intérêt pour le poste.");
        }

        long professionalCount = countMatches(text, professionalWords);
        if (professionalCount >= 3) {
            professionnalisme += 3;
            pointsForts.add("Le ton professionnel est bien présent.");
        } else if (professionalCount >= 1) {
            professionnalisme += 1;
        } else {
            aAmeliorer.add("Le contenu professionnel reste limité.");
            suggestions.add("Ajouter des compétences, expériences ou réalisations concrètes.");
        }

        Set<String> jobKeywords = extractKeywords(job + " " + description);
        long jobMatches = jobKeywords.stream()
                .filter(k -> k.length() > 2 && text.contains(k))
                .count();

        if (jobMatches >= 5) {
            pertinence += 4;
            pointsForts.add("La lettre est bien alignée avec le poste visé.");
        } else if (jobMatches >= 2) {
            pertinence += 2;
            pointsForts.add("La lettre présente une certaine adéquation avec le poste.");
        } else {
            aAmeliorer.add("La lettre n'est pas assez personnalisée pour le poste ciblé.");
            suggestions.add("Ajouter des éléments directement liés au poste et à l'entreprise.");
        }

        if (text.contains("madame") || text.contains("monsieur")) {
            clarte += 1;
            professionnalisme += 1;
            pointsForts.add("La structure formelle de la lettre est respectée.");
        }

        if (text.contains("je souhaite") || text.contains("je voudrais") || text.contains("je suis convaincu")) {
            originalite += 1;
            motivation += 1;
        }

        if (text.contains("projet") || text.contains("stage") || text.contains("expérience")) {
            originalite += 2;
            pointsForts.add("La lettre mentionne des éléments concrets du parcours.");
        } else {
            aAmeliorer.add("La lettre manque d'exemples concrets.");
            suggestions.add("Mentionner un projet, une mission ou une expérience pertinente.");
        }

        pertinence = clamp(pertinence, 0, 10);
        clarte = clamp(clarte, 0, 10);
        motivation = clamp(motivation, 0, 10);
        professionnalisme = clamp(professionnalisme, 0, 10);
        originalite = clamp(originalite, 0, 10);

        int scoreGlobal = (int) Math.round(
                (pertinence + clarte + motivation + professionnalisme + originalite) * 2.0
        );

        String verdict = computeVerdict(scoreGlobal);

        if (pointsForts.isEmpty()) {
            pointsForts.add("La lettre contient une base exploitable.");
        }

        if (aAmeliorer.isEmpty()) {
            aAmeliorer.add("Quelques améliorations mineures peuvent encore renforcer la lettre.");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Personnaliser davantage la lettre selon le poste visé.");
        }

        AnalysisResult result = new AnalysisResult();
        result.setScoreGlobal(scoreGlobal);
        result.setScores(Map.of(
                "pertinence", pertinence,
                "clarte", clarte,
                "motivation", motivation,
                "professionnalisme", professionnalisme,
                "originalite", originalite
        ));
        result.setPointsForts(pointsForts);
        result.setAAmeliorer(aAmeliorer);
        result.setSuggestions(suggestions);
        result.setVerdict(verdict);
        result.setLangue("Français");

        log.info("✅ Analyse locale générée — scoreGlobal={}, verdict={}", scoreGlobal, verdict);

        return result;
    }

    private String computeVerdict(int scoreGlobal) {
        if (scoreGlobal >= 80) return "EXCELLENT";
        if (scoreGlobal >= 60) return "BON";
        if (scoreGlobal >= 40) return "MOYEN";
        return "INSUFFISANT";
    }

    private long countMatches(String text, List<String> words) {
        return words.stream().filter(text::contains).count();
    }

    private Set<String> extractKeywords(String text) {
        Set<String> stopWords = Set.of(
                "le", "la", "les", "de", "du", "des", "un", "une", "et", "ou", "pour",
                "dans", "sur", "avec", "par", "au", "aux", "en", "d", "l", "a", "à",
                "poste", "cible", "aucune", "fournie", "entreprise"
        );

        return Arrays.stream(text.split("\\W+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> s.length() > 2)
                .filter(s -> !stopWords.contains(s))
                .collect(Collectors.toSet());
    }

    private String cleanJson(String rawJson) {
        String clean = rawJson.trim()
                .replaceAll("(?s)^```json\\s*", "")
                .replaceAll("(?s)^```\\s*", "")
                .replaceAll("(?s)\\s*```$", "")
                .trim();

        int start = clean.indexOf('{');
        int end = clean.lastIndexOf('}');
        if (start >= 0 && end > start) {
            clean = clean.substring(start, end + 1);
        }

        return clean;
    }

    private boolean isValidResult(AnalysisResult result) {
        if (result == null) return false;
        if (result.getScoreGlobal() < 0 || result.getScoreGlobal() > 100) return false;
        if (result.getScores() == null || result.getScores().isEmpty()) return false;
        return result.getVerdict() != null && !result.getVerdict().isBlank();
    }

    private AnalysisResult buildFallbackResult(String reason, String suggestion) {
        AnalysisResult result = new AnalysisResult();
        result.setScoreGlobal(0);
        result.setScores(Map.of(
                "pertinence", 0,
                "clarte", 0,
                "motivation", 0,
                "professionnalisme", 0,
                "originalite", 0
        ));
        result.setPointsForts(List.of());
        result.setAAmeliorer(List.of(reason));
        result.setSuggestions(new ArrayList<>(List.of(suggestion)));
        result.setVerdict("INSUFFISANT");
        result.setLangue("Français");
        return result;
    }

    private void sleepBeforeRetry(int currentIndex) {
        if (currentIndex >= MODELS.length - 1) {
            return;
        }

        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrompu");
        }
    }

    private int readInt(JsonNode node, String field, int defaultValue) {
        JsonNode value = node.get(field);
        return (value != null && value.isNumber()) ? value.asInt() : defaultValue;
    }

    private String readText(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        return (value != null && !value.isNull()) ? value.asText(defaultValue) : defaultValue;
    }

    private List<String> readStringList(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (JsonNode item : value) {
            if (item != null && !item.isNull()) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private List<String> readStringListWithAliases(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isArray()) {
                return readStringList(node, field);
            }
        }
        return List.of();
    }

    private String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}