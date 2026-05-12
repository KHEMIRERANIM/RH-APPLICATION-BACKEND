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
public class GlobalSentimentAnalysis {
    private int totalAvis;
    private double scoreMoyen;
    private String sentimentGlobal;
    private Map<String, Long> sentimentDistribution;
    private Map<String, Double> tendances;
    private List<String> suggestions;
    private List<SentimentResult> avisAnalysees;

    // ✅ Getters explicites (Lombok les génère normalement, mais au cas où)
    public int getTotalAvis() { return totalAvis; }
    public double getScoreMoyen() { return scoreMoyen; }
    public String getSentimentGlobal() { return sentimentGlobal; }
    public Map<String, Long> getSentimentDistribution() { return sentimentDistribution; }
    public Map<String, Double> getTendances() { return tendances; }
    public List<String> getSuggestions() { return suggestions; }
    public List<SentimentResult> getAvisAnalysees() { return avisAnalysees; }
}