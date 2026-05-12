// ParticipantInscription.java
package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "inscriptions_formation")
public class ParticipantInscription {

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

    @Field("statut")
    private String statut = "INSCRIT"; // INSCRIT, PRESENT, ABSENT, VALIDE

    @Field("presence_validee")
    private Boolean presenceValidee = false;

    @Field("date_inscription")
    private LocalDateTime dateInscription;

    @Field("date_presence")
    private LocalDateTime datePresence;

    @Field("commentaire")
    private String commentaire;

    // ✅ AJOUTER CES CHAMPS POUR LES INFORMATIONS DE LA FORMATION
    @Field("formation_titre")
    private String formationTitre;

    @Field("formateur")
    private String formateur;

    @Field("lieu")
    private String lieu;

    @Field("lien_visio")
    private String lienVisio;

    @Field("lien_google_maps")
    private String lienGoogleMaps;

    @Field("date_debut")
    private LocalDateTime dateDebut;

    @Field("date_fin")
    private LocalDateTime dateFin;

    @Field("duree_heures")
    private Integer dureeHeures;

    @Field("type")
    private String type;

    @Field("niveau")
    private String niveau;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    // ✅ AJOUTER CES CHAMPS
    @Field("motif_annulation")
    private String motifAnnulation;

    @Field("date_annulation")
    private LocalDateTime dateAnnulation;

    @Field("points_rembourses")
    private int pointsRembourses;


}