// OllamaService.java
package tn.esprit.rh_rse.service.Formation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ollama.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.temperature}")
    private double temperature;

    @Value("${ollama.max-tokens}")
    private int maxTokens;

    private WebClient webClient;
    private final Map<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder
                .baseUrl(ollamaUrl)
                .build();
    }

    public Mono<String> generateResponse(String prompt, String sessionId) {
        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "temperature", temperature,
                        "num_predict", maxTokens
                )
        );

        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode json = objectMapper.readTree(response);
                        return json.get("response").asText();
                    } catch (Exception e) {
                        log.error("Erreur parsing réponse Ollama", e);
                        return "Désolé, une erreur est survenue.";
                    }
                })
                .onErrorResume(e -> {
                    log.error("Erreur appel Ollama", e);
                    return Mono.just("Le service IA est temporairement indisponible.");
                });
    }

    public String buildPrompt(String question, List<FormationKnowledge> context, String sessionId) {
        StringBuilder prompt = new StringBuilder();

        // Contexte système
        prompt.append("Tu es un assistant spécialisé dans les formations professionnelles. ");
        prompt.append("Réponds uniquement en français, de manière précise et professionnelle.\n\n");

        // Historique de conversation (contexte)
        List<Map<String, String>> history = conversationHistory.getOrDefault(sessionId, List.of());
        if (!history.isEmpty()) {
            prompt.append("Historique de la conversation récente:\n");
            history.stream()
                    .skip(Math.max(0, history.size() - 5)) // Derniers 5 échanges
                    .forEach(exchange -> {
                        prompt.append("Utilisateur: ").append(exchange.get("question")).append("\n");
                        prompt.append("Assistant: ").append(exchange.get("answer")).append("\n");
                    });
            prompt.append("\n");
        }

        // Contexte de la base de connaissances
        prompt.append("Voici les formations disponibles dans notre catalogue:\n\n");
        for (int i = 0; i < context.size(); i++) {
            FormationKnowledge f = context.get(i);
            prompt.append(i + 1).append(". Formation: ").append(f.getTitre()).append("\n");
            prompt.append("   Description: ").append(f.getDescription()).append("\n");
            prompt.append("   Objectifs: ").append(f.getObjectifs()).append("\n");
            prompt.append("   Prérequis: ").append(f.getPreRequis()).append("\n");
            prompt.append("   Durée: ").append(f.getDureeHeures()).append(" heures\n\n");
        }

        // Question de l'utilisateur
        prompt.append("Question de l'utilisateur: ").append(question).append("\n\n");
        prompt.append("Réponse: ");

        return prompt.toString();
    }

    public void addToHistory(String sessionId, String question, String answer) {
        conversationHistory.computeIfAbsent(sessionId, k -> new java.util.ArrayList<>())
                .add(Map.of("question", question, "answer", answer));

        // Limiter l'historique à 20 échanges pour éviter la surcharge mémoire
        List<Map<String, String>> history = conversationHistory.get(sessionId);
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }
    }

    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
    }
}