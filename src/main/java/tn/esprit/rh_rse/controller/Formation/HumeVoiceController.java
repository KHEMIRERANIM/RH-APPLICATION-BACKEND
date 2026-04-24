package tn.esprit.rh_rse.controller.Formation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/hume")
@RequiredArgsConstructor
@Slf4j
public class HumeVoiceController {

    @Value("${hume.api.key}")
    private String humeApiKey;

    @Value("${hume.api.secret}")
    private String humeSecretKey;

    @Value("${hume.api.ws-url}")
    private String humeWsUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Analyse un fichier audio via l'API Hume
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeVoice(
            @RequestParam("audio") MultipartFile audioFile) {

        try {
            log.info("🎤 Analyse vocale via Hume AI - Taille: {} bytes", audioFile.getSize());

            // Convertir l'audio en base64
            byte[] audioBytes = audioFile.getBytes();
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);

            // Appeler l'API Hume
            Map<String, Object> result = callHumeAPI(base64Audio);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Erreur analyse vocale", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Génère un token temporaire pour le frontend (WebSocket)
     */
    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getAccessToken() {
        // Pour Hume, on peut générer un JWT ou utiliser directement la clé
        // Recommandé: créer un token JWT avec expiration courte
        String token = generateTemporaryToken();
        return ResponseEntity.ok(Map.of(
                "token", token,
                "wsUrl", humeWsUrl
        ));
    }

    /**
     * Appel direct à l'API Hume
     */
    private Map<String, Object> callHumeAPI(String base64Audio) {
        // Configuration des headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", humeApiKey);

        // Corps de la requête
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", base64Audio);
        requestBody.put("models", Map.of(
                "prosody", Map.of(),      // Analyse du ton
                "language", Map.of()      // Analyse du texte
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Appel à l'API Hume
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.hume.ai/v0/batch/jobs",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response.getBody());

            return result;

        } catch (Exception e) {
            log.error("Erreur appel Hume API", e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    private String generateTemporaryToken() {
        // Générer un token JWT avec expiration (15 minutes)
        // Pour simplifier, on retourne la clé API (À sécuriser en production)
        return humeApiKey;
    }
}