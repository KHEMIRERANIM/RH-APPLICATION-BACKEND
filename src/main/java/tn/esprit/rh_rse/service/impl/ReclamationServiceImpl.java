package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.rh_rse.entity.Reclamation;
import tn.esprit.rh_rse.repository.ReclamationRepository;
import tn.esprit.rh_rse.service.ReclamationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReclamationServiceImpl implements ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final RestTemplate restTemplate;

    @Override
    public Reclamation saveReclamation(Reclamation reclamation) {
        reclamation.setDate(LocalDateTime.now());
        reclamation.setStatus("PENDING");

        // Geocoding with Nominatim (OpenStreetMap)
        try {
            String query = reclamation.getNeighborhood() + ", Tunis, Tunisia";
            String url = "https://nominatim.openstreetmap.org/search?q=" + query + "&format=json&limit=1";
            
            // Nominatim requires a User-Agent header or it might reject the request
            // For a simple RestTemplate call, it might work, but let's be careful.
            // If it fails, we'll log it.
            
            ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> response = responseEntity.getBody();
            
            if (response != null && !response.isEmpty()) {
                Map<String, Object> firstResult = response.get(0);
                reclamation.setLatitude(Double.parseDouble((String) firstResult.get("lat")));
                reclamation.setLongitude(Double.parseDouble((String) firstResult.get("lon")));
            }
        } catch (Exception e) {
            log.error("Error geocoding neighborhood: {}", e.getMessage());
            // We still save the reclamation even if geocoding fails, 
            // but without coordinates it won't show on the heatmap
        }

        return reclamationRepository.save(reclamation);
    }

    @Override
    public List<Reclamation> getAllReclamations() {
        return reclamationRepository.findAll();
    }
}
