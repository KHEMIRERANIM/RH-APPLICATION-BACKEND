// OllamaService.java
package tn.esprit.rh_rse.service.Formation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // VALEURS HARDCODEES DIRECTEMENT
    private String ollamaUrl = "http://localhost:11434";
    private String model = "llama2";
    private double temperature = 0.7;
    private int maxTokens = 500;

    private WebClient webClient;
    private final Map<String, List<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing OllamaService with URL: {}, Model: {}", ollamaUrl, model);
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

        prompt.append("Tu es un assistant spécialisé dans les formations professionnelles. ");
        prompt.append("Réponds uniquement en français, de manière précise et professionnelle.\n\n");

        List<Map<String, String>> history = conversationHistory.getOrDefault(sessionId, List.of());
        if (!history.isEmpty()) {
            prompt.append("Historique de la conversation récente:\n");
            history.stream()
                    .skip(Math.max(0, history.size() - 5))
                    .forEach(exchange -> {
                        prompt.append("Utilisateur: ").append(exchange.get("question")).append("\n");
                        prompt.append("Assistant: ").append(exchange.get("answer")).append("\n");
                    });
            prompt.append("\n");
        }

        prompt.append("Voici les formations disponibles dans notre catalogue:\n\n");
        for (int i = 0; i < context.size(); i++) {
            FormationKnowledge f = context.get(i);
            prompt.append(i + 1).append(". Formation: ").append(f.getTitre()).append("\n");
            prompt.append("   Description: ").append(f.getDescription()).append("\n");
            prompt.append("   Objectifs: ").append(f.getObjectifs()).append("\n");
            prompt.append("   Prérequis: ").append(f.getPreRequis()).append("\n");
            prompt.append("   Durée: ").append(f.getDureeHeures()).append(" heures\n\n");
        }

        prompt.append("Question de l'utilisateur: ").append(question).append("\n\n");
        prompt.append("Réponse: ");

        return prompt.toString();
    }

    public void addToHistory(String sessionId, String question, String answer) {
        conversationHistory.computeIfAbsent(sessionId, k -> new java.util.ArrayList<>())
                .add(Map.of("question", question, "answer", answer));

        List<Map<String, String>> history = conversationHistory.get(sessionId);
        if (history.size() > 20) {
            history.subList(0, history.size() - 20).clear();
        }
    }

    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
    }
}