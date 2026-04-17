package tn.esprit.rh_rse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrgenceDto {
    private boolean urgence;
    private double probabilite_rupture;
}
