package tn.esprit.rh_rse.dto.request;

import lombok.*;
import tn.esprit.rh_rse.entity.enums.StatutReservation;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationNavetteRequest {
    private String busId;
    private String employeId;
    private LocalDate dateTrajet;
    private StatutReservation statut;
    private String joursSelectionnes;    // "Mar,Mer"

}