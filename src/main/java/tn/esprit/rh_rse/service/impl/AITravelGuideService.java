package tn.esprit.rh_rse.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.rh_rse.entity.OffreAvantage;
import tn.esprit.rh_rse.exception.OffreAvantageNotFoundException;
import tn.esprit.rh_rse.repository.OffreAvantageRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AITravelGuideService {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final OffreAvantageRepository offreAvantageRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String genererProgramme(String idOffreAvantage) {
        OffreAvantage offre = offreAvantageRepository.findById(idOffreAvantage)
                .orElseThrow(() -> new OffreAvantageNotFoundException(idOffreAvantage));

        String prompt = construirePrompt(offre);
        return appelerGroqAPI(prompt);
    }

    private String construirePrompt(OffreAvantage offre) {
        String titre = offre.getTitre() != null ? offre.getTitre() : "Inconnu";
        String lieu = offre.getLocalisation() != null ? offre.getLocalisation() : "la destination";
        
        long nbJours = 3;
        if (offre.getDateDebut() != null && offre.getDateFin() != null) {
            nbJours = ChronoUnit.DAYS.between(offre.getDateDebut(), offre.getDateFin());
            if (nbJours <= 0) nbJours = 1;
        }

        String type = offre.getCategorie() != null ? offre.getCategorie().name() : "VOYAGE";

        if ("HOTEL".equalsIgnoreCase(type) || "SEJOUR".equalsIgnoreCase(type)) {
            return String.format(
                "Tu es un concierge d'hôtel de luxe expert. L'utilisateur séjourne à '%s' situé à '%s' pour %d jours. " +
                "Génère un programme parfait comprenant 3 attractions/activités proches incontournables et 3 recommandations de restaurants locaux typiques. " +
                "IMPORTANT: Renvoie UNIQUEMENT un format JSON pur (pas de markdown, pas de texte avant ou après). " +
                "Le JSON doit avoir cette structure stricte : " +
                "{ \"type\": \"HOTEL\", \"attractions\": [ {\"nom\": \"...\", \"description\": \"...\", \"icon\": \"🏛️\"} ], \"restaurants\": [ {\"nom\": \"...\", \"specialite\": \"...\", \"icon\": \"🍽️\"} ] }",
                titre, lieu, nbJours
            );
        } else {
            return String.format(
                "Tu es un guide touristique expert. Crée un itinéraire de %d jours pour un voyage à '%s' intitulé '%s'. " +
                "IMPORTANT: Renvoie UNIQUEMENT un format JSON pur (pas de markdown, pas de texte avant ou après). " +
                "Le JSON doit avoir cette structure stricte : " +
                "{ \"type\": \"VOYAGE\", \"jours\": [ {\"jour\": 1, \"titre\": \"...\", \"matin\": \"...\", \"apresMidi\": \"...\", \"soir\": \"...\"} ] }",
                nbJours, lieu, titre
            );
        }
    }

    private String appelerGroqAPI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.3-70b-versatile");
        requestBody.put("messages", List.of(message));
        requestBody.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(GROQ_API_URL, entity, String.class);
            
            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            
            // Clean up possible markdown code blocks (```json ... ```)
            content = content.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("\\s*```$", "").trim();
            
            return content;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du programme IA : " + e.getMessage());
        }
    }
}
