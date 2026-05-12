// ChatbotService.java - Version corrigée
package tn.esprit.rh_rse.service.Formation;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.Formation.ChatbotResponseDTO;
import tn.esprit.rh_rse.entity.Formation.Formation;
import tn.esprit.rh_rse.repository.Formation.FormationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final FormationRepository formationRepository;
    private final HuggingFaceService huggingFaceService;

    @PostConstruct
    public void init() {
        loadFormationsIntoKnowledgeBase();
    }

    public void loadFormationsIntoKnowledgeBase() {
        List<Formation> formations = formationRepository.findAll();

        List<FormationKnowledge> knowledgeBase = formations.stream()
                .filter(f -> f.isActive())
                .map(this::convertToKnowledge)
                .collect(Collectors.toList());

        huggingFaceService.initializeKnowledgeBase(knowledgeBase);
        log.info("Base de connaissances chargée avec {} formations actives", knowledgeBase.size());
    }

    private FormationKnowledge convertToKnowledge(Formation formation) {
        return FormationKnowledge.builder()
                .id(formation.getId())
                .titre(formation.getTitre())
                .description(formation.getDescription())
                .objectifs(formation.getObjectifs())
                .preRequis(formation.getPreRequis())
                .type(formation.getType())
                .niveau(formation.getNiveau())
                .formateur(formation.getFormateur())
                .dureeHeures(formation.getDureeHeures())
                .nombrePlaces(formation.getNombrePlaces())
                .build();
    }

    // ✅ Méthode avec sessionId
    public ChatbotResponseDTO askQuestion(String question, String sessionId) {
        if (question == null || question.trim().isEmpty()) {
            return ChatbotResponseDTO.builder()
                    .success(false)
                    .answer("Veuillez poser une question valide.")
                    .sources(List.of())
                    .build();
        }
        return huggingFaceService.answerQuestion(question, sessionId);
    }

    // ✅ Garder aussi l'ancienne méthode pour compatibilité (optionnel)
    public ChatbotResponseDTO askQuestion(String question) {
        return askQuestion(question, "default");
    }

}