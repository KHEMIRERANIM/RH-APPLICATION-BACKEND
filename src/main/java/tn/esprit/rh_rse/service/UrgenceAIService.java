package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.rh_rse.dto.request.OffrePredictionRequestDto;
import tn.esprit.rh_rse.dto.response.UrgenceDto;
import tn.esprit.rh_rse.entity.Offre;
import tn.esprit.rh_rse.exception.OffreNotFoundException;
import tn.esprit.rh_rse.repository.OffreRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrgenceAIService {

    private final OffreRepository offreRepository;

    // URL of our local Python Microservice
    private final String AI_MICROSERVICE_URL = "http://127.0.0.1:8000/predict-urgency";

    public UrgenceDto evaluerUrgence(String idOffre) {
        Offre offre = offreRepository.findById(idOffre)
                .orElseThrow(() -> new OffreNotFoundException("Offre non trouvée avec l'ID: " + idOffre));

        // 1. Calcul des variables métiers pour l'IA
        Double prix = offre.getPrixReel();
        if (offre.getPrixConvention() != null) {
            prix = offre.getPrixConvention();
        } else if (prix == null) {
            prix = 100.0; // Prix par défaut si tout est null
        }

        int moisEvenement = (offre.getDateDebut() != null) ? offre.getDateDebut().getMonthValue() : LocalDate.now().getMonthValue();
        
        long joursAvantDebut = 30; // Valeur par défaut
        if (offre.getDateDebut() != null) {
            joursAvantDebut = ChronoUnit.DAYS.between(LocalDate.now(), offre.getDateDebut());
            if (joursAvantDebut < 0) joursAvantDebut = 1; // L'événement a déjà commencé
        }

        int placesInitiales = (offre.getNbPlacesTotal() != null && offre.getNbPlacesTotal() > 0) ? offre.getNbPlacesTotal() : 50;
        int placesRestantes = (offre.getNbPlacesDispo() != null) ? offre.getNbPlacesDispo() : placesInitiales;
        
        String categorie = (offre.getCategorie() != null) ? offre.getCategorie().name() : "HOTEL";

        // 2. Construction de l'objet à envoyer vers Python
        OffrePredictionRequestDto requestDto = OffrePredictionRequestDto.builder()
                .prix(prix)
                .mois_evenement(moisEvenement)
                .jours_avant_debut((int) joursAvantDebut)
                .places_initiales(placesInitiales)
                .places_restantes(placesRestantes)
                .categorie(categorie)
                .build();

        // 3. Appel HTTP vers FastAPI (Python)
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<OffrePredictionRequestDto> requestEntity = new HttpEntity<>(requestDto, headers);

            UrgenceDto response = restTemplate.postForObject(AI_MICROSERVICE_URL, requestEntity, UrgenceDto.class);
            log.info("L'IA a répondu: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("Erreur de communication avec le microservice IA: {}", e.getMessage());
            // Mécanisme de Fallback si l'IA est hors-ligne
            return UrgenceDto.builder().urgence(false).probabilite_rupture(0.0).build();
        }
    }
}
