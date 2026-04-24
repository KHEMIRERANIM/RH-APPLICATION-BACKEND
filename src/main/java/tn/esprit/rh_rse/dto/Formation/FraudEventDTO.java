// dto/Formation/FraudEventDTO.java
package tn.esprit.rh_rse.dto.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudEventDTO {
    private String examenId;
    private String employeId;
    private String employeNom;
    private String employePrenom;
    private String employeEmail;
    private String eventType;
    private Long timestamp;
    private String description;
}