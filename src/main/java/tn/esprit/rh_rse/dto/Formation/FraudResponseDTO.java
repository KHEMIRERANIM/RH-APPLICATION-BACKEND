// dto/Formation/FraudResponseDTO.java
package tn.esprit.rh_rse.dto.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudResponseDTO {
    private boolean saved;
    private long totalViolations;
    private boolean blocked;
    private String message;
    private int remainingAttempts;
}