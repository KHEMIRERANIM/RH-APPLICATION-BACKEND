// dto/FraudSummaryDTO.java
package tn.esprit.rh_rse.dto.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudSummaryDTO {
    private String employeId;
    private String employeNom;
    private String employePrenom;
    private Long violationCount;
    private Integer maxRiskScore;
    private Boolean isBlocked;
    private Boolean examenBloque;
}