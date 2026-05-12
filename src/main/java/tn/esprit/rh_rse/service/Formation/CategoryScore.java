package tn.esprit.rh_rse.service.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryScore {
    private String category;
    private int mentions;
    private double sentimentScore;
}