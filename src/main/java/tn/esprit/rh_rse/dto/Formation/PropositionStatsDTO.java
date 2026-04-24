// dto/Formation/PropositionStatsDTO.java
package tn.esprit.rh_rse.dto.Formation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropositionStatsDTO {

    private long enAttente;
    private long validees;
    private long rejetees;
    private long programmees;
    private long totalInterets;

    private Map<String, Long> statsParType;
    private Map<String, Double> scoreMoyenParSource;
    private Map<String, Long> propositionsParMois;

    private double tauxValidation;
    private double tauxTransformation; // Propositions validées → Programmation
    private int nombreEmployesUniquesInteresses;
}