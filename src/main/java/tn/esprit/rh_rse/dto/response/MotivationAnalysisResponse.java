package tn.esprit.rh_rse.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MotivationAnalysisResponse {
    private int scoreGlobal;
    private int scorePertinence;
    private int scoreClarte;
    private int scoreMotivation;
    private int scoreProfessionnalisme;
    private int scoreOriginalite;
    private String sentiment;
    private String langue;
    private List<String> pointsForts;
    private List<String> pointsAmeliorer;
    private List<String> suggestions;
    private String resume;
    private String recommandation;
    private String recommandationColor;
}