package tn.esprit.rh_rse.entity;

import lombok.*;
import java.util.List;
import java.util.Map;

/**
 * Sous-document optionnel présent UNIQUEMENT pour les offres de catégorie HOTEL.
 * Permet de gérer la tarification différenciée adultes/enfants et les formules pension.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailsHotel {

    /** Prix convention par adulte et par nuit */
    private Double prixAdulte;

    /** Prix convention par enfant et par nuit */
    private Double prixEnfant;

    /** Âge limite pour le tarif enfant (ex: 12 = enfant de moins de 12 ans) */
    private Integer ageLimiteEnfant;

    /** Durée du séjour en nuits */
    private Integer nombreNuits;

    /**
     * Formules pension disponibles pour cette offre.
     * Valeurs possibles : "PD" (Petit Déjeuner), "DP" (Demi Pension), "PC" (Pension Complète)
     */
    private List<String> formulesDisponibles;

    /**
     * Surprix par personne et par nuit pour chaque formule.
     * Ex: { "PD": 0.0, "DP": 15.0, "PC": 28.0 }
     */
    private Map<String, Double> surprixFormules;

    /** Informations sur les types de chambres proposées (optionnel) */
    private String typeChambres;
}
