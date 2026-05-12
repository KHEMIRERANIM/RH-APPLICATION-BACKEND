// ResultatExamen.java - Version MongoDB (sans annotations JPA)
package tn.esprit.rh_rse.entity.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resultats_examens")
public class ResultatExamen {

    @Id
    private String id;

    @Field("examen_id")
    private String examenId;

    @Field("employe_id")
    private String employeId;

    @Field("employe_nom")
    private String employeNom;

    @Field("employe_prenom")
    private String employePrenom;

    @Field("employe_email")
    private String employeEmail;  // Ajouté pour l'export CSV

    @Field("note")
    private Double note;

    @Field("reponses")
    private List<Reponse> reponses;

    @Field("submitted_at")
    private LocalDateTime submittedAt;

    @Field("valide")
    private Boolean valide = false;

    @Field("feedback_ia")
    private String feedbackIA;  // Feedback détaillé par question (texte global ou JSON)

    @Field("correction_detaillee")
    private String correctionDetaillee;  // JSON avec détails par question

    @Field("temps_prise_ms")
    private Long tempsPriseMs;  // Optionnel : temps passé sur l'examen

    @Field("ia_utilisee")
    private Boolean iaUtilisee = false;  // Indique si l'IA a été utilisée pour la correction
}