package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.StatutReservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "avantage_reservation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvantageReservation {

    @Id
    private String id;

    private String idUser;
    private String idOffre;

    /** Nombre total de personnes (adultes + enfants) */
    private Integer nbPersonnes;

    /** Prix unitaire de référence (prixConvention pour non-hôtel, prixAdulte pour hôtel) */
    private Double prixUnitaire;
    private Double prixTotal;
    private StatutReservation statut;
    private LocalDateTime dateReservation;
    private LocalDateTime dateAnnulation;

    // ── Champs spécifiques HOTEL (null pour VOYAGE/FESTIVAL) ──────────
    /** Nombre d'adultes (offres hôtelières uniquement) */
    private Integer nbAdultes;

    /** Nombre d'enfants (offres hôtelières uniquement) */
    private Integer nbEnfants;

    /**
     * Formule pension choisie : "PD", "DP", ou "PC"
     * (offres hôtelières uniquement)
     */
    private String formule;

    /** Date d'arrivée (Check-in) pour les réservations d'hôtel */
    private LocalDate checkIn;

    /** Date de départ (Check-out) pour les réservations d'hôtel */
    private LocalDate checkOut;
}
