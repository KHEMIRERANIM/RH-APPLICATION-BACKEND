package tn.esprit.rh_rse.dto.response;

import lombok.Builder;
import lombok.Data;
import tn.esprit.rh_rse.entity.enums.StatutEntretien;
import tn.esprit.rh_rse.entity.enums.TypeEntretien;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EntretienResponse {
    private String id;
    private String candidatureId;
    private String recruteurId;
    private TypeEntretien type;
    private LocalDateTime dateHeure;
    private Integer dureeMinutes;
    private String lieu;
    private String lienVisio;
    private StatutEntretien statut;
    private String feedbackGlobal;
    private Integer noteGlobale;
    private List<String> pointsForts;
    private List<String> pointsFaibles;
    private boolean recommandeEmbauche;
    private LocalDateTime createdAt;
}