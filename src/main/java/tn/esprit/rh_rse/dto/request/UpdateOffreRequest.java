package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import tn.esprit.rh_rse.entity.enums.TypeContrat;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateOffreRequest {
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
    private int nombrePostes;
    private LocalDateTime dateExpiration;
}