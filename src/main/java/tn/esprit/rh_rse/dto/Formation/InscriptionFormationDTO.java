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
}