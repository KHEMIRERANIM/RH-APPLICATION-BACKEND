package tn.esprit.rh_rse.service.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LexicalAnalysis {
    private int positifCount;
    private int negatifCount;
    private int totalSentimentWords;
    private double netScore;
    private List<String> motsPositifs;
    private List<String> motsNegatifs;
}