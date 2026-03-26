package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.time.LocalDateTime;

@Document(collection = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    private String id;

    private String idUser;
    private String idOffre;
    private Integer nbPersonnes;
    private Double prixUnitaire;
    private Double prixTotal;
    private StatutReservation statut;
    private LocalDateTime dateReservation;
    private LocalDateTime dateAnnulation;
}