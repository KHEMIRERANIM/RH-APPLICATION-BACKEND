package tn.esprit.rh_rse.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeRemplacementCovoiturageRequest {
    private String ancienneReservationId;
    private String nouveauTrajetId;
    private String employeId;
    private Double distanceKm;
}
