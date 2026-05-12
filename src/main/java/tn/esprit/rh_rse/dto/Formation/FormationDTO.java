package tn.esprit.rh_rse.dto.Formation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FormationDTO {
    private String id;
    private String titre;
    private String description;
    private String objectifs;
    private String preRequis;
    private String type;
    private Integer dureeHeures;
    private Integer nombrePlaces;
    private Integer placesDisponibles;
    private String niveau;
    private String formateur;
    private String formateurBio;
    private String lieu;
    private String lienVisio;
    private String lienGoogleMaps;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDateTime dateLimiteInscription;
    private String imageUrl;
    private Boolean active;
    private Double noteMoyenne;
    private Integer nombreInscrits;

    // ==================== NOUVEAUX CHAMPS POUR PRÉREQUIS ====================
    private String prerequisFormationId;
    private String prerequisFormationTitre;
}