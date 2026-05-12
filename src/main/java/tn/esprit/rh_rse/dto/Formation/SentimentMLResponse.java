package tn.esprit.rh_rse.dto.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentMLResponse {
    private String emotion;
    private double sentimentScore;
    private int suggestedRating;
    private double confidence;
    private String transcript;
    private double[] classProbabilities;
}

