package tn.esprit.rh_rse.dto.Formation;
// dto/Formation/RecommendationDTO.java

import lombok.Data;

import java.util.List;

@Data
public class RecommendationDTO {
    private String technologie;
    private Double score;
    private String source;
    private String priorite;
    private List<String> suggestionsFormations;
}
