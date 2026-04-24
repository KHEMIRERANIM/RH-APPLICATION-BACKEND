package tn.esprit.rh_rse.dto.Formation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InscriptionFormationDTO {
    private String id;
    private String formationId;
    private String formationTitre;
    private String employeId;
    private String employeNom;
    private String statut;
    private LocalDateTime dateInscription;
    private String motifAnnulation;
    private Boolean presenceConfirmee;
    private LocalDateTime datePresence;

    // Informations de la formation
    private String formateur;
    private String lieu;
    private String lienVisio;
    private String lienGoogleMaps;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Integer dureeHeures;
}