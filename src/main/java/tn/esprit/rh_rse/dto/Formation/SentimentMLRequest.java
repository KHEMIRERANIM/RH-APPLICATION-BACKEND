package tn.esprit.rh_rse.dto.Formation;

import lombok.Data;

@Data
public class SentimentMLRequest {
    private String text;
    private Integer noteUtilisateur;
    private String formationId;
}