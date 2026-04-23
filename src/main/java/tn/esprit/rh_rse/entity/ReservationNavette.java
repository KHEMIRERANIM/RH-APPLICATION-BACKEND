package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "reservations_navette")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationNavette {

    @Id
    private String id;

    private String busId;

    private String employeId;

    private LocalDateTime dateReservation;

    private StatutReservation statut;

    private String ligne;

    private String heureDepart;

    private Integer dureeMinutes;

    private LocalDate dateTrajet;

    private Double co2EconomiseKg;

    private Integer pointsEco;

    private LocalDate dateCalcul;
    private String joursSelectionnes;    // "Mar,Mer" (mardi et mercredi)

    private Double distanceKm;

    private List<String> joursEnAttente;
    private List<String> joursConfirmes;
}