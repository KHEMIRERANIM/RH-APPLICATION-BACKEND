package tn.esprit.rh_rse.dto.response;

import lombok.Builder;
import lombok.Data;
import tn.esprit.rh_rse.entity.enums.StatutOffre;
import tn.esprit.rh_rse.entity.enums.TypeContrat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OffreResponse {
    private String id;
    private String titre;
    private String description;
    private String departement;
    private String localisation;
    private TypeContrat typeContrat;
    private String niveauExperience;
    private String niveauEtudes;
    private Double salaireMin;
    private Double salaireMax;
    private List<String> competencesRequises;
    private List<String> avantages;
    private StatutOffre statut;
    private boolean biasDetected;
    private String createurId;
    private int nombrePostes;
    private int nombreCandidatures;
    private LocalDateTime dateCreation;
    private LocalDateTime datePublication;
    private LocalDateTime dateExpiration;
}