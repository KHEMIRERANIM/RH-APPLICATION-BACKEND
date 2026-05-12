package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "formations")
@Data
public class Formation {

    @Id
    private String id;

    @Field("titre")
    private String titre;

    @Field("description")
    private String description;

    @Field("objectifs")
    private String objectifs;

    @Field("pre_requis")
    private String preRequis;

    @Field("type")
    private String type;

    @Field("duree_heures")
    private Integer dureeHeures;

    @Field("nombre_places")
    private Integer nombrePlaces;

    @Field("places_disponibles")
    private Integer placesDisponibles;

    @Field("niveau")
    private String niveau;

    @Field("formateur")
    private String formateur;

    @Field("formateur_bio")
    private String formateurBio;

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

    @Field("date_limite_inscription")
    private LocalDateTime dateLimiteInscription;

    @Field("image_url")
    private String imageUrl;

    @Field("active")
    private Boolean active = true;
    private List<DocumentFormation> documents;
    private List<Examen> examens;
    @Field("formateur_id")
    private String formateurId; // ID du formateur assigné


    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;


    // ==================== NOUVEAUX CHAMPS POUR PRÉREQUIS ====================

    @Field("prerequis_formation_id")
    private String prerequisFormationId;
    @Field("note_moyenne")
    private Double noteMoyenne = 0.0;

    @Field("prerequis_formation_titre")
    private String prerequisFormationTitre;

    public boolean isActive() {
        return active != null && active;
    }
}