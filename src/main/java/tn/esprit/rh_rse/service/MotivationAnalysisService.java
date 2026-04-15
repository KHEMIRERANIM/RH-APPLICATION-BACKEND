package tn.esprit.rh_rse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.rh_rse.dto.response.MotivationAnalysisResponse;

import java.util.*;

@Slf4j
@Service
public class MotivationAnalysisService {

    @Value("${flask.analyzer.url}")
    private String flaskUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public MotivationAnalysisResponse analyze(
            String letterBase64,
            String targetPost,
            String currentPost,
            String employeeName) {

        try {
            Map<String, String> body = new HashMap<>();
            body.put("letterBase64", letterBase64);
            body.put("targetPost",   targetPost);
            body.put("currentPost",  currentPost);
            body.put("employeeName", employeeName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<MotivationAnalysisResponse> response = restTemplate.postForEntity(
                    flaskUrl + "/analyze",
                    entity,
                    MotivationAnalysisResponse.class
            );

            return response.getBody() != null ? response.getBody() : buildErrorResponse();

        } catch (Exception e) {
            log.error("Erreur Flask: {}", e.getMessage());
            return buildErrorResponse();
        }
    }

    private MotivationAnalysisResponse buildErrorResponse() {
        return MotivationAnalysisResponse.builder()
                .scoreGlobal(0).scorePertinence(0).scoreClarte(0)
                .scoreMotivation(0).scoreProfessionnalisme(0).scoreOriginalite(0)
                .sentiment("NEUTRE").langue("fr")
                .pointsForts(List.of())
                .pointsAmeliorer(List.of("Analyse indisponible"))
                .suggestions(List.of("Verifier que Flask est demarré sur le port 5000"))
                .resume("Service IA indisponible")
                .recommandation("MITIGE")
                .recommandationColor("#d97706")
                .build();
    }
}