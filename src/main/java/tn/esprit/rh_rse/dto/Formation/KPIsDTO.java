package tn.esprit.rh_rse.dto.Formation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KPIsDTO {
    // KPIs classiques
    private Double tauxRemplissageMoyen;
    private Double tauxReussiteGlobal;
    private Integer totalInscrits;
    private Integer totalCertifies;
    private Integer totalAbandons;
    private Double npsMoyen;
    private Double chiffreAffairesTotal;
    private Map<String, Double> tauxRemplissageParFormation;
    private Map<String, Double> tauxReussiteParFormation;
    private EmployeActifDTO employeLePlusActif;

    // KPI IA - Prédiction d'échec
    private Double tauxRisqueEchecGlobal;
    private Map<String, Double> risqueEchecParFormation;
    private Map<String, AlerteFormation> alertesFormations;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AlerteFormation {
        private Double risque;
        private String recommandation;
        private String action;
    }


    // dto/Formation/KPIsDTO.java - Ajouter cette classe interne
    @Data
    public static class EmployeActifDTO {
        private String employeId;
        private String nom;
        private String prenom;
        private String email;
        private Integer nombreInscriptions;
        private List<String> formationsSuivies;
    }
}
