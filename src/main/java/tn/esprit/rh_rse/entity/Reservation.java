package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    private String id;
    private LocalDateTime dateAcceptation;  // ← AJOUTE CETTE LIGNE

    private String trajetId;

    private String employeId;

    private LocalDateTime dateReservation;

    private StatutReservation statut;

    // Champs CO2 & points écologiques
    private Double co2AvecCovoit;

    private Double co2EconomiseKg;

    private Integer pointsEco;

    private LocalDate dateCalcul;

    private Double distanceKm; // ← ajouter ce champ

    /** Si présent : cette réservation remplacera l'ancienne (même employé) après confirmation conducteur */
    private String remplaceReservationId;

}
