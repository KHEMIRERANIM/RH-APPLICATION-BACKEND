package tn.esprit.rh_rse.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffrePredictionRequestDto {
    private Double prix;
    private Integer mois_evenement;
    private Integer jours_avant_debut;
    private Integer places_initiales;
    private Integer places_restantes;
    private String categorie;
}
