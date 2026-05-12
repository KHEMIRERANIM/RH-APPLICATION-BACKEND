package tn.esprit.rh_rse.entity.Formation;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
@Builder
@Data
@Document(collection = "avis_formations")
public class Avis {

    @Id
    private String id;

    @Field("formation_id")
    private String formationId;

    @Field("employe_id")
    private String employeId;

    @Field("employe_nom")
    private String employeNom;

    @Field("employe_prenom")
    private String employePrenom;

    @Field("employe_email")
    private String employeEmail;

    @Field("note")
    private Integer note; // 1 à 5 étoiles

    @Field("commentaire")
    private String commentaire;

    @Field("titre")
    private String titre;

    @Field("valide")
    private Boolean valide = false; // Validation par le formateur

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}