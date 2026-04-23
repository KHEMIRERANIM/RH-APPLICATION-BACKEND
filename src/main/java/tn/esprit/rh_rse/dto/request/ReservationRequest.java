package tn.esprit.rh_rse.dto.request;


import lombok.*;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    private String trajetId;
    private String employeId;
    private StatutReservation statut;
    private Double distanceKm; // ← ajoute cette ligne

}