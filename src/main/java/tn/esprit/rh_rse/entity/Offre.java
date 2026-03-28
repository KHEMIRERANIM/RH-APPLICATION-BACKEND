package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.CategorieOffre;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "offres")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offre {

    @Id
    private String id;

    private String idPartenaire;
    private String titre;
    private String description;
    private CategorieOffre categorie;

    /** Prix réel (barré) — toujours présent pour toutes les catégories */
    private Double prixReel;

    /**
     * Prix convention — UNIQUEMENT pour VOYAGE et FESTIVAL.
     * Pour HOTEL, ce champ est null ; les prix sont dans detailsHotel.
     */
    private Double prixConvention;

    private Integer nbPlacesTotal;
    private Integer nbPlacesDispo;
    private String imageUrl;
    private String localisation;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String statut; // "ACTIVE" ou "INACTIVE"
    private LocalDateTime createdAt;

    /**
     * Détails spécifiques aux offres hôtelières (null pour VOYAGE et FESTIVAL).
     * Contient la tarification adulte/enfant, les formules pension et le nombre de nuits.
     */
    private DetailsHotel detailsHotel;
}