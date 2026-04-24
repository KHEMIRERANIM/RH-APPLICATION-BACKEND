package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "evaluations_formation")
@Data
public class EvaluationFormation {

    @Id
    private String id;

    @Field("formation_id")
    private String formationId;

    @Field("employe_id")
    private String employeId;

    @Field("note")
    private Integer note; // 1-5

    @Field("commentaire")
    private String commentaire;

    @Field("recommandation")
    private Boolean recommandation;

    @Field("points_positifs")
    private String pointsPositifs;

    @Field("points_amelioration")
    private String pointsAmelioration;

    @Field("date_evaluation")
    private LocalDateTime dateEvaluation;
}