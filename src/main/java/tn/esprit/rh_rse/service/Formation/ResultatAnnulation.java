package tn.esprit.rh_rse.service.Formation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultatAnnulation {
    private boolean succes;
    private int pointsRembourses;
    private String message;
    private LocalDateTime dateAnnulation;
}