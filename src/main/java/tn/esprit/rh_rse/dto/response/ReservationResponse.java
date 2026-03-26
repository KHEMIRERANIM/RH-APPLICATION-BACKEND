package tn.esprit.rh_rse.dto.response;


import lombok.*;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private String id;
    private String trajetId;
    private String employeId;
    private StatutReservation statut;
    private LocalDateTime dateReservation;
    private Double co2AvecCovoit;
    private Double co2EconomiseKg;
    private Integer pointsEco;
    private LocalDate dateCalcul;
}