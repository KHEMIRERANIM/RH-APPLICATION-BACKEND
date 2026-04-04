package tn.esprit.rh_rse.dto.response;

import lombok.*;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationNavetteResponse {
    private String id;
    private String busId;
    private String employeId;
    private StatutReservation statut;
    private LocalDateTime dateReservation;
    private String ligne;
    private String heureDepart;
    private Integer dureeMinutes;
    private Integer pointsEco;
    private Double co2EconomiseKg;
    private Double distanceKm;
    private String joursSelectionnes;    // ← AJOUTE CETTE LIGNE

}