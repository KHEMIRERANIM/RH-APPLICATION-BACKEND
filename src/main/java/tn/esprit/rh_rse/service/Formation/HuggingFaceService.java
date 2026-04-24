// HuggingFaceService.java - Version avec priorité aux réponses éducatives
package tn.esprit.rh_rse.service.Formation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tn.esprit.rh_rse.dto.Formation.ChatbotResponseDTO;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HuggingFaceService {

    @Value("${huggingface.api.key:}")
    private String apiKey;

    private String huggingFaceUrl;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private List<FormationKnowledge> knowledgeBase = new ArrayList<>();

    // Technologies connues avec leurs descriptions
    private static final Map<String, Map<String, String>> TECH_KNOWLEDGE = new HashMap<>();

    static {
        // Angular
        Map<String, String> angularInfo = new HashMap<>();
        angularInfo.put("type", "Framework JavaScript/TypeScript");
        angularInfo.put("creator", "Google");
        angularInfo.put("description", "Angular est un framework puissant pour construire des applications web dynamiques et modernes.");
        angularInfo.put("features", "• Composants réutilisables\n• Injection de dépendances\n• Data binding bidirectionnel\n• Routing intégré\n• Formulaires réactifs\n• PWA ready");
        angularInfo.put("learning", "• Les composants Angular\n• Les services et l'injection\n• Le routing\n• Les formulaires\n• Les observables RxJS\n• Les directives personnalisées");
        TECH_KNOWLEDGE.put("angular", angularInfo);

        // React
        Map<String, String> reactInfo = new HashMap<>();
        reactInfo.put("type", "Bibliothèque JavaScript");
        reactInfo.put("creator", "Meta (Facebook)");
        reactInfo.put("description", "React est une bibliothèque pour créer des interfaces utilisateur interactives avec des composants réutilisables.");
        reactInfo.put("features", "• Composants fonctionnels\n• Hooks (useState, useEffect)\n• JSX\n• DOM virtuel\n• Performance optimisée");
        reactInfo.put("learning", "• Les composants React\n• Les props et le state\n• Les hooks\n• React Router\n• Context API\n• Redux");
        TECH_KNOWLEDGE.put("react", reactInfo);

        // Java
        Map<String, String> javaInfo = new HashMap<>();
        javaInfo.put("type", "Langage de programmation orienté objet");
        javaInfo.put("creator", "Sun Microsystems (Oracle)");
        javaInfo.put("description", "Java est un langage de programmation robuste, multiplateforme et très utilisé en entreprise.");
        javaInfo.put("features", "• Plateforme indépendante (JVM)\n• Gestion automatique de la mémoire\n• Multi-threading intégré\n• Riche écosystème\n• Sécurité renforcée");
        javaInfo.put("learning", "• Syntaxe Java\n• Programmation orientée objet\n• Collections\n• Exceptions\n• Streams et lambdas\n• JDBC");
        TECH_KNOWLEDGE.put("java", javaInfo);

        // Python
        Map<String, String> pythonInfo = new HashMap<>();
        pythonInfo.put("type", "Langage de programmation interprété");
        pythonInfo.put("creator", "Guido van Rossum");
        pythonInfo.put("description", "Python est un langage polyvalent, simple à apprendre et très utilisé en data science.");
        pythonInfo.put("features", "• Syntaxe claire et lisible\n• Typage dynamique\n• Large bibliothèque standard\n• Idéal pour l'IA/ML\n• Multi-paradigme");
        pythonInfo.put("learning", "• Syntaxe Python\n• Structures de données\n• Fonctions et modules\n• POO en Python\n• Bibliothèques (pandas, numpy)\n• APIs avec FastAPI");
        TECH_KNOWLEDGE.put("python", pythonInfo);

        // Spring Boot
        Map<String, String> springInfo = new HashMap<>();
        springInfo.put("type", "Framework Java pour applications d'entreprise");
        springInfo.put("creator", "Pivotal (VMware)");
        springInfo.put("description", "Spring Boot simplifie le développement d'applications Java avec configuration automatique.");
        springInfo.put("features", "• Auto-configuration\n• Microservices ready\n• Sécurité intégrée\n• REST APIs faciles\n• Intégration avec bases de données");
        springInfo.put("learning", "• Spring Boot basics\n• REST Controllers\n• Spring Data JPA\n• Spring Security\n• Microservices\n• Déploiement");
        TECH_KNOWLEDGE.put("spring", springInfo);
        TECH_KNOWLEDGE.put("springboot", springInfo);
    }

    @PostConstruct
    public void init() {
        this.huggingFaceUrl = "https://api-inference.huggingface.co/models/microsoft/phi-2";
        log.info(" HuggingFace Service initialisé");
    }

    public void initializeKnowledgeBase(List<FormationKnowledge> formations) {
        this.knowledgeBase = new ArrayList<>(formations);
        log.info(" Base de connaissances initialisée avec {} formations", formations.size());
    }

    /**
     * Détecte si la question demande une explication éducative sur une technologie
     */
    private boolean isEducationalQuestion(String question) {
        String lowerQ = question.toLowerCase();
        String[] educationalPatterns = {
                "c'est quoi", "c est quoi", "qu'est-ce que", "qu est ce que",
                "quoi", "définition", "definition", "signifie", "language",
                "langage", "framework", "bibliothèque", "librairie", "c'est un",
                "c est un", "est-ce que", "explique", "presente", "présente"
        };

        for (String pattern : educationalPatterns) {
            if (lowerQ.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extrait la technologie dont on parle
     */
    private String extractTechnology(String question) {
        String lowerQ = question.toLowerCase();

        // Chercher les technologies connues
        for (String tech : TECH_KNOWLEDGE.keySet()) {
            if (lowerQ.contains(tech)) {
                return tech;
            }
        }

        // Patterns pour extraire le nom
        String[] patterns = {
                "c'est quoi ([a-z]+)",
                "c est quoi ([a-z]+)",
                "qu'est-ce que ([a-z]+)",
                "qu est ce que ([a-z]+)",
                "quoi ([a-z]+)",
                "([a-z]+) c'est quoi",
                "([a-z]+) c est quoi",
                "([a-z]+) language",
                "([a-z]+) langage",
                "([a-z]+) framework",
                "signifie ([a-z]+)"
        };

        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(lowerQ);
            if (m.find()) {
                String tech = m.group(1);
                if (tech.length() > 2 && tech.length() < 30) {
                    return tech;
                }
            }
        }

        return null;
    }

    /**
     * Génère une réponse éducative sur une technologie
     */
    private String generateEducationalAnswer(String technology, String question) {
        String tech = technology.toLowerCase();
        Map<String, String> info = TECH_KNOWLEDGE.get(tech);

        if (info != null) {
            StringBuilder answer = new StringBuilder();

            // Titre et introduction
            answer.append(" **").append(technology.toUpperCase()).append("**\n\n");
            answer.append(info.get("description")).append("\n\n");

            answer.append(" **Type :** ").append(info.get("type")).append("\n");
            answer.append(" **Créé par :** ").append(info.get("creator")).append("\n\n");

            answer.append("⚡ **Caractéristiques principales :**\n");
            answer.append(info.get("features")).append("\n\n");

            // Vérifier si la formation existe
            Optional<FormationKnowledge> formation = knowledgeBase.stream()
                    .filter(f -> f.getTitre().toLowerCase().equals(tech))
                    .findFirst();

            if (formation.isPresent()) {
                answer.append("🎓 **Formation ").append(technology).append(" chez nous :**\n");
                answer.append("• Durée : ").append(formation.get().getDureeHeures()).append(" heures\n");
                answer.append("• Type : ").append(formation.get().getType()).append("\n");
                if (formation.get().getObjectifs() != null && !formation.get().getObjectifs().isEmpty()) {
                    answer.append("• Objectifs : ").append(formation.get().getObjectifs()).append("\n");
                }
                answer.append("\n");
            } else {
                answer.append("🎓 **Ce que vous apprendrez dans notre formation ").append(technology).append(" :**\n");
                answer.append(info.get("learning")).append("\n\n");
            }

            answer.append("💡 **Pour aller plus loin :**\n");
            answer.append("• Consultez notre catalogue de formations\n");
            answer.append("• Parlez avec notre équipe pédagogique\n");
            answer.append("• Inscrivez-vous à la prochaine session\n\n");

            answer.append("✨ **Autres questions sur ").append(technology).append(" ?** (prérequis, dates, formateur...)");

            return answer.toString();
        } else {
            // Technologie non répertoriée
            return generateGenericEducationalAnswer(technology);
        }
    }

    /**
     * Réponse générique pour une technologie non répertoriée
     */
    private String generateGenericEducationalAnswer(String technology) {
        return "🔍 **" + technology.toUpperCase() + "**\n\n" +
                technology.toUpperCase() + " est une technologie utilisée dans le développement informatique.\n\n" +
                " **Dans notre catalogue :**\n" +
                "Nous proposons des formations sur " + technology + " pour vous aider à maîtriser cet outil.\n\n" +
                " **Pour en savoir plus :**\n" +
                "• Consultez notre catalogue de formations\n" +
                "• Contactez notre service RH\n" +
                "• Demandez un programme détaillé\n\n" +
                "✨ Souhaitez-vous connaître les dates des prochaines formations sur " + technology + " ?";
    }

    /**
     * Génère une réponse sur une formation existante
     */
    private String generateFormationAnswer(FormationKnowledge formation, String question) {
        String lowerQ = question.toLowerCase();

        // Si question éducative, donner la réponse éducative
        if (isEducationalQuestion(question)) {
            String tech = extractTechnology(question);
            if (tech != null && !tech.isEmpty()) {
                return generateEducationalAnswer(tech, question);
            }
        }

        // Prérequis
        if (lowerQ.contains("prérequis") || lowerQ.contains("prerequis") || lowerQ.contains("condition")) {
            if (formation.getPreRequis() != null && !formation.getPreRequis().isEmpty()) {
                return "📋 **Prérequis pour " + formation.getTitre() + "** :\n\n" + formation.getPreRequis();
            } else {
                return "📋 **Prérequis pour " + formation.getTitre() + "** :\n\nAucun prérequis spécifique n'est nécessaire. Formation ouverte à tous !";
            }
        }

        // Objectifs
        if (lowerQ.contains("objectif") || lowerQ.contains("but") || lowerQ.contains("apprendre")) {
            if (formation.getObjectifs() != null && !formation.getObjectifs().isEmpty()) {
                return "🎯 **Objectifs de " + formation.getTitre() + "** :\n\n" + formation.getObjectifs();
            } else {
                return "🎯 **Objectifs de " + formation.getTitre() + "** :\n\nCette formation vous permettra de maîtriser " + formation.getTitre() +
                        " et de l'appliquer concrètement dans vos projets professionnels.";
            }
        }

        // Formateur
        if (lowerQ.contains("formateur") || lowerQ.contains("animateur") || lowerQ.contains("qui anime")) {
            if (formation.getFormateur() != null && !formation.getFormateur().isEmpty()) {
                return " **" + formation.getTitre() + "** est animée par **" + formation.getFormateur() + "**, un expert dans le domaine.";
            } else {
                return " Le formateur pour **" + formation.getTitre() + "** sera communiqué prochainement. Ce sont généralement des experts certifiés.";
            }
        }

        // Durée
        if (lowerQ.contains("durée") || lowerQ.contains("duree") || lowerQ.contains("longueur") || lowerQ.contains("temps")) {
            return " La formation **" + formation.getTitre() + "** dure **" + formation.getDureeHeures() + " heures**.";
        }

        // Réponse complète par défaut
        StringBuilder answer = new StringBuilder();
        answer.append(" **").append(formation.getTitre()).append("**\n\n");

        if (formation.getDescription() != null && !formation.getDescription().isEmpty()) {
            answer.append("📝 ").append(formation.getDescription()).append("\n\n");
        } else {
            answer.append("📝 Formation technique sur **").append(formation.getTitre()).append("** pour développer vos compétences.\n\n");
        }

        answer.append("🏷️ **Type :** ").append(formation.getType()).append("\n");
        answer.append(" **Durée :** ").append(formation.getDureeHeures()).append(" heures\n");

        if (formation.getObjectifs() != null && !formation.getObjectifs().isEmpty()) {
            answer.append("\n **Objectifs :** ").append(formation.getObjectifs()).append("\n");
        }

        answer.append("\n❓ **Questions possibles :**\n");
        answer.append("• Quels sont les prérequis ?\n");
        answer.append("• Qui est le formateur ?\n");
        answer.append("• Quand aura lieu la prochaine session ?");

        return answer.toString();
    }

    private List<FormationKnowledge> searchRelevantFormations(String question, int topK) {
        if (knowledgeBase.isEmpty()) {
            return new ArrayList<>();
        }

        String cleanQuestion = question.toLowerCase()
                .replaceAll("[éèêë]", "e")
                .replaceAll("[àâä]", "a")
                .replaceAll("[ôö]", "o")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9\\s]", "");

        String[] words = cleanQuestion.split("\\s+");

        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "le", "la", "les", "un", "une", "des", "du", "de", "et", "ou", "pour",
                "dans", "avec", "sans", "sur", "par", "est", "sont", "ce", "cette",
                "ces", "concernant", "quoi", "sert"
        ));

        List<String> keywords = Arrays.stream(words)
                .filter(w -> w.length() > 2)
                .filter(w -> !stopWords.contains(w))
                .collect(Collectors.toList());

        if (keywords.isEmpty()) {
            return knowledgeBase.stream().limit(topK).collect(Collectors.toList());
        }

        List<ScoredFormation> scored = new ArrayList<>();
        for (FormationKnowledge formation : knowledgeBase) {
            int score = calculateScore(formation, keywords);
            if (score > 0) {
                scored.add(new ScoredFormation(formation, score));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        return scored.stream()
                .filter(s -> s.score >= 10)
                .limit(topK)
                .map(s -> s.formation)
                .collect(Collectors.toList());
    }

    private int calculateScore(FormationKnowledge formation, List<String> keywords) {
        String text = String.format("%s %s %s %s %s",
                formation.getTitre(),
                formation.getDescription() != null ? formation.getDescription() : "",
                formation.getObjectifs() != null ? formation.getObjectifs() : "",
                formation.getType() != null ? formation.getType() : "",
                formation.getPreRequis() != null ? formation.getPreRequis() : ""
        ).toLowerCase();

        int score = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                score += 10;
            }
            if (formation.getTitre().toLowerCase().contains(keyword)) {
                score += 30;
            }
        }
        return score;
    }

    public ChatbotResponseDTO answerQuestion(String question, String sessionId) {
        try {
            log.info("🔍 Question: {}", question);

            // 1. PRIORITÉ : Vérifier si c'est une question éducative sur une technologie
            if (isEducationalQuestion(question)) {
                String technology = extractTechnology(question);
                if (technology != null && !technology.isEmpty()) {
                    String answer = generateEducationalAnswer(technology, question);
                    return ChatbotResponseDTO.builder()
                            .success(true)
                            .answer(answer)
                            .sources(List.of(technology))
                            .build();
                }
            }

            // 2. Rechercher dans la base de formations
            List<FormationKnowledge> relevant = searchRelevantFormations(question, 3);

            if (!relevant.isEmpty()) {
                FormationKnowledge formation = relevant.get(0);
                String answer = generateFormationAnswer(formation, question);
                return ChatbotResponseDTO.builder()
                        .success(true)
                        .answer(answer)
                        .sources(List.of(formation.getTitre()))
                        .build();
            }

            // 3. Réponse par défaut utile
            return ChatbotResponseDTO.builder()
                    .success(true)
                    .answer(generateHelpfulResponse(question))
                    .sources(List.of())
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ChatbotResponseDTO.builder()
                    .success(true)
                    .answer("🤔 Je suis désolé, je n'ai pas pu traiter votre question.\n\n💡 **Essayez :**\n• \"C'est quoi Angular ?\"\n• \"Prérequis pour Java\"\n• \"Liste des formations\"")
                    .sources(List.of())
                    .build();
        }
    }

    private String generateHelpfulResponse(String question) {
        return "🤖 **Assistant Formations**\n\n" +
                "Je peux vous aider sur :\n\n" +
                " **Découvrir les technologies :**\n" +
                "• \"C'est quoi Angular ?\"\n" +
                "• \"Présente-moi React\"\n" +
                "• \"Java c'est un langage ?\"\n\n" +
                "🎓 **Nos formations :**\n" +
                "• \"Liste des formations disponibles\"\n" +
                "• \"Prérequis pour la formation Python\"\n" +
                "• \"Objectifs de la formation Spring\"\n\n" +
                "📝 **Inscription :**\n" +
                "• \"Comment m'inscrire ?\"\n" +
                "• \"Système de points\"\n\n" +
                "Comment puis-je vous aider aujourd'hui ?";
    }

    public void recomputeAllEmbeddings() {
        // Non utilisé
    }

    @lombok.AllArgsConstructor
    private static class ScoredFormation {
        FormationKnowledge formation;
        int score;
    }
}