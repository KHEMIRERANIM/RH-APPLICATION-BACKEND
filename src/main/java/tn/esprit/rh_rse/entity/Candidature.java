package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.StatutCandidature;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "candidatures")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidature {

    @Id
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
    private String comparaisonExplication;
    private Integer anneesExperienceDetecte;
    
    // Video Test Langue
    private Boolean testLanguePasse;
    private Double scoreLangue;
    private Boolean formationRequise;
    
    // Soft Skills / RSE
    private Integer scoreLeadership;
    private Integer scoreEmpathie;
    private Integer scoreAdaptabilite;
    private Integer scoreCommunication;
    private Integer scoreInnovation;

    private LocalDateTime datePostulation;
    private LocalDateTime dateDerniereMAJ;
}