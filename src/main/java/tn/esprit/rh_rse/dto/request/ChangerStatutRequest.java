package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import tn.esprit.rh_rse.entity.enums.StatutCandidature;

@Data
public class ChangerStatutRequest {
    private StatutCandidature nouveauStatut;
    private String commentaire;
}