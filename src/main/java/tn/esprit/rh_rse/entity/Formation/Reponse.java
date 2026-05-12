// Reponse.java - Ajouter le feedback par question
package tn.esprit.rh_rse.entity.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reponse {

    @Field("question_id")
    private String questionId;

    @Field("reponse")
    private String reponse;

    @Field("points_obtenus")
    private double pointsObtenus;

    @Field("feedback")
    private String feedback;  // ✅ Feedback spécifique à cette question

    @Field("commentaire_ia")
    private String commentaireIa;  // ✅ Commentaire détaillé de l'IA
}