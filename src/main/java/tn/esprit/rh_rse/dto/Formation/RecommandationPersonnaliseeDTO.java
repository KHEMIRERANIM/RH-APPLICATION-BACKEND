
package tn.esprit.rh_rse.dto.Formation;

import lombok.Data;

import java.util.List;

@Data
public class RecommandationPersonnaliseeDTO {
    private String employeId;
    private List<String> technologiesConnues;
    private List<String> technologiesRecommandees;
    private String prochainNiveau;
    private List<FormationSuggestion> suggestionsFormations;


    @Data
    public static class FormationSuggestion {
        private String formationId;
        private String titre;
        private String description;
        private Integer score;
        private String raison;
    }
}