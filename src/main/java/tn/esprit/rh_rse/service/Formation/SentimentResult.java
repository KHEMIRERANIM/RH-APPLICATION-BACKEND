package tn.esprit.rh_rse.service.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentResult {
    private String avisId;
    private double sentimentScore;
    private String sentiment;
    private Integer suggestedRating;
    private double confiance;
    private LexicalAnalysis lexicalAnalysis;
    private Map<String, CategoryScore> categoryScores;
    private double intensite;
    private boolean anomaly;
    private List<String> recommendations;
    private List<String> keywords;
    private int noteUtilisateur;
    private double ecartNoteScore;

    // ✅ Getters explicites
    public String getAvisId() { return avisId; }
    public double getSentimentScore() { return sentimentScore; }
    public String getSentiment() { return sentiment; }
    public Integer getSuggestedRating() { return suggestedRating; }
    public double getConfiance() { return confiance; }
    public LexicalAnalysis getLexicalAnalysis() { return lexicalAnalysis; }
    public boolean isAnomaly() { return anomaly; }
    public List<String> getRecommendations() { return recommendations; }
    public List<String> getKeywords() { return keywords; }
    public double getEcartNoteScore() { return ecartNoteScore; }
}