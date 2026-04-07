package tn.esprit.rh_rse.dto.response;

import lombok.Builder;
import lombok.Data;
import tn.esprit.rh_rse.entity.enums.StatutCandidature;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CandidatureResponse {
    private String id;
    private String candidatId;
    private String offreId;
    private String cvFileId;
    private String lettreMotivationFileId;
    private StatutCandidature statut;
    private Double scoreMatching;
    private String etapeActuelle;
    private String notesRecruteur;
    private List<String> historiqueStatuts;
    private List<String> competencesExtraites;
    private List<String> competencesManquantes;
    private Integer anneesExperienceDetecte;
    private Boolean testLanguePasse;
    private Double scoreLangue;
    private Boolean formationRequise;
    private LocalDateTime datePostulation;
    private LocalDateTime dateDerniereMAJ;
}