package tn.esprit.rh_rse.service.Formation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    // ============================================================
    //  MÉTHODE PRINCIPALE — correction intelligente
    // ============================================================

    public CorrectionIA evaluerReponseIntelligente(
            String question,
            String reponseAttendue,
            String reponseUtilisateur,
            String typeQuestion,
            int pointsMax) {

        // --- garde-fous de base ---
        if (reponseUtilisateur == null || reponseUtilisateur.isBlank()) {
            return CorrectionIA.builder()
                    .pointsObtenus(0)
                    .feedback(" Aucune réponse fournie.")
                    .commentaire("L'étudiant n'a pas répondu à cette question.")
                    .build();
        }

        // --- vérification locale rapide ---
        CorrectionIA localResult = verifierLocalement(reponseUtilisateur, reponseAttendue, pointsMax, question);
        if (localResult != null) {
            return localResult;
        }

        // --- appel Gemini ---
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                return appellerGemini(question, reponseAttendue, reponseUtilisateur, typeQuestion, pointsMax);
            } catch (Exception e) {
                log.error("❌ Erreur Gemini, bascule sur évaluation locale : {}", e.getMessage());
            }
        }

        return evaluationSemantiqueLocale(reponseUtilisateur, reponseAttendue, question, pointsMax);
    }

    // ============================================================
    //  VÉRIFICATION LOCALE RAPIDE (CORRIGÉE)
    // ============================================================

    private CorrectionIA verifierLocalement(String reponseUtilisateur, String reponseAttendue, int pointsMax, String question) {
        String rep    = normaliser(reponseUtilisateur);
        String attend = normaliser(reponseAttendue);

        String originalRep = reponseUtilisateur.trim();
        String originalAttend = reponseAttendue.trim();

        // 1. Correspondance exacte (insensible à la casse)
        if (rep.equals(attend)) {
            return buildReponseCorrecte(pointsMax, originalRep, originalAttend,
                    " Exactement la réponse attendue.");
        }

        // 2. Vérification des abréviations et synonymes
        Map<String, List<String>> synonymes = buildSynonymes();
        for (Map.Entry<String, List<String>> entry : synonymes.entrySet()) {
            String canonical = entry.getKey();

            // Vérifie si la réponse attendue correspond au canonique
            if (!attend.contains(canonical) && !canonical.contains(attend)) {
                continue;
            }

            for (String variante : entry.getValue()) {
                String varianteNorm = normaliser(variante);
                if (rep.equals(varianteNorm)) {
                    return buildReponseCorrecte(pointsMax, originalRep, originalAttend,
                            String.format("✅ '%s' est une abréviation valide de '%s'.", originalRep, originalAttend));
                }
            }
        }

        // 3. La réponse attendue contient la réponse (ex: "Js" contenu dans "JavaScript")
        if (attend.contains(rep) && rep.length() >= 2) {
            return buildReponseCorrecte(pointsMax, originalRep, originalAttend,
                    String.format("✅ '%s' est une abréviation ou forme courte acceptée de '%s'.", originalRep, originalAttend));
        }

        // 4. La réponse contient la réponse attendue
        if (rep.contains(attend)) {
            return buildReponseCorrecte(pointsMax, originalRep, originalAttend,
                    String.format("✅ Votre réponse contient la notion attendue '%s'.", originalAttend));
        }

        return null; // Pas de correspondance locale → laisser Gemini décider
    }

    /**
     * Construit une réponse de correction avec explication détaillée (pour les réponses correctes)
     */
    private CorrectionIA buildReponseCorrecte(int pointsMax, String reponseUser, String reponseAttendue, String explication) {
        return CorrectionIA.builder()
                .pointsObtenus(pointsMax)
                .feedback("✅ Correct !")
                .commentaire(String.format("""
                        📝 Votre réponse : "%s"
                        📖 Réponse attendue : "%s"
                        
                        %s
                        
                        💡 Explication : TypeScript est un sur-ensemble de JavaScript.
                        Le compilateur TypeScript (tsc) transforme le code TypeScript 
                        en code JavaScript standard exécutable par les navigateurs et Node.js.
                        """, reponseUser, reponseAttendue, explication))
                .build();
    }

    /**
     * Construit une réponse de correction avec explication détaillée (pour les réponses incorrectes ou partielles)
     * CORRECTION : paramètre double au lieu de int
     */
    private CorrectionIA buildReponseIncorrecte(double pointsObtenus, String reponseUser, String reponseAttendue, String raison) {
        return CorrectionIA.builder()
                .pointsObtenus(pointsObtenus)
                .feedback(pointsObtenus == 0 ? "❌ Incorrect" : "⚠️ Partiellement correct")
                .commentaire(String.format("""
                        📝 Votre réponse : "%s"
                        📖 Réponse attendue : "%s"
                        
                        ❌ Raison : %s
                        
                        💡 Rappel : TypeScript se compile TOUJOURS en JavaScript.
                        """, reponseUser, reponseAttendue, raison))
                .build();
    }

    /** Table des synonymes/abréviations (AMÉLIORÉE avec toutes les variantes de casse) */
    private Map<String, List<String>> buildSynonymes() {
        Map<String, List<String>> m = new LinkedHashMap<>();

        m.put("typescript",   Arrays.asList("ts", "ts ", " type script", "typescript (ts)"));
        m.put("javascript",   Arrays.asList("js", "Js", "JS", "j s", "java script", "ecmascript", "es6", "ecma script"));
        m.put("angular",      Arrays.asList("ang", "angularjs", "angular.js"));
        m.put("react",        Arrays.asList("reactjs", "react.js", "react js"));
        m.put("vue",          Arrays.asList("vuejs", "vue.js", "vue js"));
        m.put("python",       Arrays.asList("py", "python3", "python 3"));
        m.put("java",         Arrays.asList("java se", "java ee", "jdk"));
        m.put("spring boot",  Arrays.asList("springboot", "spring-boot", "spring"));
        m.put("html",         Arrays.asList("html5", "hyper text markup language"));
        m.put("css",          Arrays.asList("css3", "cascading style sheets"));
        m.put("sql",          Arrays.asList("structured query language", "mysql", "postgresql", "postgres"));
        m.put("api",          Arrays.asList("rest api", "restful api", "web api", "interface"));
        m.put("intelligence artificielle", Arrays.asList("ia", "ai", "artificial intelligence", "machine learning", "ml", "deep learning"));
        m.put("base de données", Arrays.asList("bdd", "db", "database", "sgbd", "sgbdr"));
        m.put("objet",        Arrays.asList("oop", "poo", "orienté objet", "object oriented"));

        return m;
    }

    // ============================================================
    //  APPEL GEMINI AVEC PROMPT AMÉLIORÉ
    // ============================================================

    private CorrectionIA appellerGemini(
            String question,
            String reponseAttendue,
            String reponseUtilisateur,
            String typeQuestion,
            int pointsMax) throws Exception {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        String prompt = buildPromptIntelligent(
                question, reponseAttendue, reponseUtilisateur, typeQuestion, pointsMax);

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", prompt);
        content.put("parts", List.of(parts));
        request.put("contents", List.of(content));

        Map<String, Object> config = new HashMap<>();
        config.put("temperature", 0.1);
        config.put("maxOutputTokens", 1024);
        request.put("generationConfig", config);

        String response = webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null || response.isBlank()) {
            throw new RuntimeException("Réponse Gemini vide");
        }

        JsonNode jsonResponse = objectMapper.readTree(response);
        String answer = jsonResponse
                .path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        return parseCorrectionReponse(answer, pointsMax, reponseUtilisateur, reponseAttendue);
    }

    // ============================================================
    //  PROMPT INTELLIGENT AMÉLIORÉ
    // ============================================================

    private String buildPromptIntelligent(
            String question,
            String reponseAttendue,
            String reponseUtilisateur,
            String typeQuestion,
            int pointsMax) {

        return String.format("""
            Tu es un professeur expert chargé de corriger des examens.
            Tu dois être INTELLIGENT, JUSTE et PÉDAGOGUE.
            
            ─────────────────────────────────────────
            QUESTION     : %s
            RÉPONSE REF  : %s
            RÉPONSE ÉLÈVE: %s
            POINTS MAX   : %d
            TYPE         : %s
            ─────────────────────────────────────────
            
            🚨 RÈGLES ABSOLUES DE CORRECTION 🚨
            
            1. TOUTE ABRÉVIATION COURANTE EST ACCEPTÉE
               - "JS", "Js", "js" → JAVASCRIPT 
               - "TS", "Ts", "ts" → TYPESCRIPT 
               - "IA", "Ai" → INTELLIGENCE ARTIFICIELLE 
               
            2. LA CASSE N'A AUCUNE IMPORTANCE
               - "javascript" = "JavaScript" = "JAVASCRIPT" 
               
            3. LA LONGUEUR N'EST PAS UN CRITÈRE
               - Une réponse d'1 mot peut être parfaite 
               
            4. EXPLICATION OBLIGATOIRE POUR CHAQUE RÉPONSE
               - Pourquoi c'est correct OU pourquoi c'est incorrect
               - Comparaison avec la réponse attendue
            
            EXEMPLES POUR "Que produit TypeScript après compilation ?" (Réf: JavaScript)
            
            | Réponse | Note | Explication |
            |---------|------|-------------|
            | JavaScript | 1/1 | Exactement la bonne réponse |
            | js | 1/1 | Abréviation standard acceptée |
            | Js | 1/1 | Abréviation standard (casse différente) |
            | Java | 0/1 | Java est un langage différent, TypeScript compile vers JavaScript |
            | Jaca | 0/1 | Faute de frappe, ne correspond à aucun langage |
            | JS | 1/1 | Abréviation standard |
            
            FORMAT DE RÉPONSE OBLIGATOIRE (JSON uniquement):
            {
                "points": <0 ou 1>,
                "feedback": "< Correct / ❌ Incorrect>",
                "commentaire": "<Explication détaillée : analyse de la réponse, comparaison avec l'attendu, pourquoi c'est juste/faux>",
                "conseil": "<Conseil pédagogique si faux, chaîne vide si correct>"
            }
            """,
                question, reponseAttendue, reponseUtilisateur, pointsMax, typeQuestion
        );
    }

    // ============================================================
    //  PARSING DE LA RÉPONSE GEMINI (AMÉLIORÉ)
    // ============================================================

    private CorrectionIA parseCorrectionReponse(String jsonResponse, int pointsMax, String reponseUser, String reponseAttendue) {
        try {
            String clean = jsonResponse.trim()
                    .replaceAll("(?s)^```json\\s*", "")
                    .replaceAll("(?s)^```\\s*", "")
                    .replaceAll("(?s)\\s*```$", "")
                    .trim();

            JsonNode node = objectMapper.readTree(clean);

            double points = node.has("points") ? node.get("points").asDouble() : 0;
            String feedback = node.has("feedback") ? node.get("feedback").asText() : "";
            String commentaire = node.has("commentaire") ? node.get("commentaire").asText() : "";
            String conseil = node.has("conseil") ? node.get("conseil").asText() : "";

            points = Math.round(points * 4) / 4.0;
            points = Math.max(0, Math.min(points, pointsMax));

            // Enrichir le commentaire si nécessaire
            if (commentaire != null && !commentaire.contains("Votre réponse")) {
                commentaire = String.format("""
                        📝 Votre réponse : "%s"
                        📖 Réponse attendue : "%s"
                        
                        %s
                        """, reponseUser, reponseAttendue, commentaire);
            }

            String feedbackComplet = feedback;
            if (conseil != null && !conseil.isBlank() && !conseil.equals("chaîne vide")) {
                feedbackComplet += " 💡 " + conseil;
            }

            return CorrectionIA.builder()
                    .pointsObtenus(points)
                    .feedback(feedbackComplet)
                    .commentaire(commentaire)
                    .build();

        } catch (Exception e) {
            log.error(" Erreur parsing JSON Gemini : {}", e.getMessage());
            return evaluationSemantiqueLocale(reponseUser, reponseAttendue, "", pointsMax);
        }
    }

    // ============================================================
    //  FALLBACK — ÉVALUATION SÉMANTIQUE LOCALE (CORRIGÉE)
    // ============================================================

    private CorrectionIA evaluationSemantiqueLocale(
            String reponseUtilisateur,
            String reponseAttendue,
            String question,
            int pointsMax) {

        String rep = normaliser(reponseUtilisateur);
        String attend = normaliser(reponseAttendue);

        // Vérification des abréviations
        if (attend.equals("javascript") && (rep.equals("js") || rep.equals("javascript"))) {
            return buildReponseCorrecte(pointsMax, reponseUtilisateur, reponseAttendue,
                    " 'js' est l'abréviation standard de JavaScript.");
        }

        if (attend.equals("typescript") && (rep.equals("ts") || rep.equals("typescript"))) {
            return buildReponseCorrecte(pointsMax, reponseUtilisateur, reponseAttendue,
                    "✅ 'ts' est l'abréviation standard de TypeScript.");
        }

        double score = calculerSimilarite(rep, attend);

        if (score >= 0.8) {
            return buildReponseCorrecte(pointsMax, reponseUtilisateur, reponseAttendue,
                    "Votre réponse correspond sémantiquement à la réponse attendue.");
        }

        if (score >= 0.5) {
            double points = Math.round(pointsMax * 0.5 * 4) / 4.0;
            return buildReponseIncorrecte(points, reponseUtilisateur, reponseAttendue,
                    "La réponse est partiellement correcte mais manque de précision.");
        }

        if (score >= 0.25) {
            double points = Math.round(pointsMax * 0.25 * 4) / 4.0;
            return buildReponseIncorrecte(points, reponseUtilisateur, reponseAttendue,
                    "La réponse effleure le sujet mais est trop vague.");
        }

        return buildReponseIncorrecte(0, reponseUtilisateur, reponseAttendue,
                String.format("La réponse '%s' ne correspond pas du tout à ce qui était attendu. TypeScript compile vers JavaScript, pas vers %s.",
                        reponseUtilisateur, reponseUtilisateur));
    }

    // ============================================================
    //  MÉTHODE POUR QUESTIONS DE DÉVELOPPEMENT
    // ============================================================

    public CorrectionIA evaluerReponseDeveloppement(
            String question,
            String reponseAttendue,
            String reponseUtilisateur,
            String typeQuestion,
            int pointsMax) {

        if (reponseUtilisateur == null || reponseUtilisateur.isBlank()) {
            return CorrectionIA.builder()
                    .pointsObtenus(0)
                    .feedback("❌ Aucune réponse fournie.")
                    .commentaire("L'étudiant n'a pas répondu à cette question.")
                    .build();
        }

        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                return appellerGeminiDeveloppement(
                        question, reponseAttendue, reponseUtilisateur, pointsMax);
            } catch (Exception e) {
                log.error("❌ Erreur Gemini développement : {}", e.getMessage());
            }
        }

        return evaluationSemantiqueLocale(reponseUtilisateur, reponseAttendue, question, pointsMax);
    }

    private CorrectionIA appellerGeminiDeveloppement(
            String question,
            String reponseAttendue,
            String reponseUtilisateur,
            int pointsMax) throws Exception {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        String prompt = String.format("""
            Tu es un professeur expert qui évalue des réponses développées.
            
            QUESTION     : %s
            ÉLÉMENTS CLÉS ATTENDUS: %s
            RÉPONSE ÉLÈVE: %s
            POINTS MAX   : %d
            
            CRITÈRES D'ÉVALUATION pour une question ouverte:
            ① Présence des concepts clés (pondération: 50%%)
            ② Exactitude des informations (pondération: 30%%)
            ③ Clarté et structure de la réponse (pondération: 20%%)
            
            IMPORTANT:
            - Plusieurs formulations peuvent être correctes pour exprimer la même idée.
            - Une réponse courte mais précise vaut mieux qu'une longue réponse vague.
            - Récompense la compréhension réelle, pas la mémorisation verbatim.
            
            FORMAT JSON UNIQUEMENT:
            {
                "points": <decimal 0 à %d>,
                "feedback": "<✅/⚠️/❌> <verdict bref>",
                "commentaire": "<analyse détaillée>",
                "conseil": "<conseil pour améliorer>"
            }
            """, question, reponseAttendue, reponseUtilisateur, pointsMax, pointsMax);

        Map<String, Object> request = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> parts = new HashMap<>();
        parts.put("text", prompt);
        content.put("parts", List.of(parts));
        request.put("contents", List.of(content));

        Map<String, Object> config = new HashMap<>();
        config.put("temperature", 0.2);
        config.put("maxOutputTokens", 1024);
        request.put("generationConfig", config);

        String response = webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null || response.isBlank()) {
            throw new RuntimeException("Réponse Gemini vide");
        }

        JsonNode jsonResponse = objectMapper.readTree(response);
        String answer = jsonResponse
                .path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        return parseCorrectionReponse(answer, pointsMax, reponseUtilisateur, reponseAttendue);
    }

    // ============================================================
    //  UTILITAIRES
    // ============================================================

    private String normaliser(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double calculerSimilarite(String rep, String attend) {
        if (rep.isBlank() || attend.isBlank()) return 0.0;

        Set<String> motsAttendus = new HashSet<>(Arrays.asList(attend.split("\\s+")));
        Set<String> stopWords = Set.of("le", "la", "les", "un", "une", "des", "de", "du", "et",
                "est", "en", "que", "qui", "pour", "sur", "dans", "au", "aux", "par", "a", "ou");
        motsAttendus.removeAll(stopWords);

        if (motsAttendus.isEmpty()) return attend.equals(rep) ? 1.0 : 0.0;

        Set<String> motsReponse = new HashSet<>(Arrays.asList(rep.split("\\s+")));

        long communs = motsAttendus.stream().filter(m ->
                motsReponse.stream().anyMatch(r -> r.contains(m) || m.contains(r))
        ).count();

        return (double) communs / motsAttendus.size();
    }

    // ============================================================
    //  DTO
    // ============================================================

    @lombok.Builder
    @lombok.Data
    public static class CorrectionIA {
        private double pointsObtenus;
        private String feedback;
        private String commentaire;
    }
}