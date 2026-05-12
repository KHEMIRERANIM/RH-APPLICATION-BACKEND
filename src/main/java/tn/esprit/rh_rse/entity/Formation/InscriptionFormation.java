package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "inscriptions_formation")
@Data
public class InscriptionFormation {

    @Id
    private String id;

    @Field("formation_id")
    private String formationId;
    private LocalDateTime updatedAt;

    @Field("employe_id")
    private String employeId;

    @Field("employe_nom")
    private String employeNom;

    @Field("statut")
    private String statut; // INSCRIT, EN_COURS, VALIDE, ECHEC

    @Field("date_inscription")
    private LocalDateTime dateInscription;

    @Field("motif_annulation")
    private String motifAnnulation;

    @Field("presence_confirmee")
    private Boolean presenceConfirmee = false;

    @Field("date_presence")
    private LocalDateTime datePresence;

    // ==================== INFORMATIONS COPIÉES DE LA FORMATION ====================

    @Field("formation_titre")
    private String formationTitre;

    @Field("formateur")
    private String formateur;
    @Field("formateur_id")
    private String formateurId;

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

    // ==================== NOUVEAUX CHAMPS POUR LA VALIDATION ====================

    @Field("date_validation")
    private LocalDateTime dateValidation;

    @Field("note")
    private Double note;

    @Field("commentaire")
    private String commentaire;

    @Field("resultats_examens")
    private List<ResultatExamen> resultatsExamens;

    @Field("documents_rendus")
    private List<DocumentRendu> documentsRendus;

    @Field("formateur_validateur")
    private String formateurValidateur; // ID du formateur qui a validé

    @Field("validation_commentaire")
    private String validationCommentaire;

    public boolean isPresenceConfirmee() {
        return presenceConfirmee != null && presenceConfirmee;
    }
}