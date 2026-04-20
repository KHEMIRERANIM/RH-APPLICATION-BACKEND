package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.rh_rse.dto.request.OffreAvantagePredictionRequestDto;
import tn.esprit.rh_rse.dto.response.UrgenceDto;
import tn.esprit.rh_rse.entity.OffreAvantage;
import tn.esprit.rh_rse.exception.OffreAvantageNotFoundException;
import tn.esprit.rh_rse.repository.OffreAvantageRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrgenceAIService {

    private final OffreAvantageRepository offreAvantageRepository;

    // URL of our local Python Microservice
    private final String AI_MICROSERVICE_URL = "http://127.0.0.1:8000/predict-urgency";

    public UrgenceDto evaluerUrgence(String idOffreAvantage) {
        OffreAvantage offreAvantage = offreAvantageRepository.findById(idOffreAvantage)
                .orElseThrow(() -> new OffreAvantageNotFoundException(
                        "OffreAvantage non trouvée avec l'ID: " + idOffreAvantage));

        // 1. Calcul des variables métiers pour l'IA
        Double prix = offreAvantage.getPrixReel();
        if (offreAvantage.getPrixConvention() != null) {
            prix = offreAvantage.getPrixConvention();
        } else if (prix == null) {
            prix = 100.0; // Prix par défaut si tout est null
        }

        int moisEvenement = (offreAvantage.getDateDebut() != null) ? offreAvantage.getDateDebut().getMonthValue()
                : LocalDate.now().getMonthValue();

        long joursAvantDebut = 30; // Valeur par défaut
        if (offreAvantage.getDateDebut() != null) {
            joursAvantDebut = ChronoUnit.DAYS.between(LocalDate.now(), offreAvantage.getDateDebut());
            if (joursAvantDebut < 0)
                joursAvantDebut = 1; // L'événement a déjà commencé
        }

        int placesInitiales = (offreAvantage.getNbPlacesTotal() != null && offreAvantage.getNbPlacesTotal() > 0)
                ? offreAvantage.getNbPlacesTotal()
                : 50;
        int placesRestantes = (offreAvantage.getNbPlacesDispo() != null) ? offreAvantage.getNbPlacesDispo()
                : placesInitiales;

        String categorie = (offreAvantage.getCategorie() != null) ? offreAvantage.getCategorie().name() : "HOTEL";

        // 2. Construction de l'objet à envoyer vers Python
        OffreAvantagePredictionRequestDto requestDto = OffreAvantagePredictionRequestDto.builder()
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
            HttpEntity<OffreAvantagePredictionRequestDto> requestEntity = new HttpEntity<>(requestDto, headers);

            UrgenceDto response = restTemplate.postForObject(AI_MICROSERVICE_URL, requestEntity, UrgenceDto.class);
            log.info("L'IA a répondu: {}", response);
            return response;

        } catch (Exception e) {
            log.debug("Microservice IA non joignable (fallback activé) : {}", e.getMessage());
            // Mécanisme de Fallback si l'IA est hors-ligne
            return UrgenceDto.builder().urgence(false).probabilite_rupture(0.0).build();
        }
    }
}
