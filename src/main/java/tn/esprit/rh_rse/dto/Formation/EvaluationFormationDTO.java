package tn.esprit.rh_rse.dto.Formation;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EvaluationFormationDTO {
    private String id;
    private String formationId;
    private String employeId;
    private Integer note;
    private String commentaire;
    private Boolean recommandation;
    private String pointsPositifs;
    private String pointsAmelioration;
    private LocalDateTime dateEvaluation;
}