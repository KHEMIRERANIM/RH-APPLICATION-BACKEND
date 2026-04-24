package tn.esprit.rh_rse.dto.response.conge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {
    private int pourcentage;
    private String risque;
    private String message;
    private String recommandation;
    private int mois;
    private int annee;
    private List<FacteurPrediction> facteurs;
    private int totalDemandesHistorique;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacteurPrediction {
        private String nom;
        private int impact;
        private String description;
    }
}
