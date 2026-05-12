package tn.esprit.rh_rse.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisResult {

    private int scoreGlobal;
    private Map<String, Integer> scores;
    private List<String> pointsForts;

    @JsonAlias({"aAmeliorer", "aameliorer"})
    private List<String> aAmeliorer;

    private List<String> suggestions;
    private String verdict;
    private String langue;

    public static AnalysisResult empty() {
        AnalysisResult result = new AnalysisResult();
        result.setScoreGlobal(0);
        result.setScores(Map.of(
                "pertinence", 0,
                "clarte", 0,
                "motivation", 0,
                "professionnalisme", 0,
                "originalite", 0
        ));
        result.setPointsForts(List.of());
        result.setAAmeliorer(List.of("Aucune lettre de motivation détectée ou erreur d'extraction"));
        result.setSuggestions(List.of("Veuillez vérifier le fichier uploadé."));
        result.setVerdict("INSUFFISANT");
        result.setLangue("Français");
        return result;
    }
}