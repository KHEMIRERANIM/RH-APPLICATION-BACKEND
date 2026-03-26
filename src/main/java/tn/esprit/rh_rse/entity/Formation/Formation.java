package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

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
    private String type; // TECHNIQUE, MANAGERIAL, SOFT_SKILLS, OBLIGATOIRE, RSE, SECURITE

    @Field("duree_heures")
    private Integer dureeHeures;

    @Field("nombre_places")
    private Integer nombrePlaces;

    @Field("places_disponibles")
    private Integer placesDisponibles;

    @Field("niveau")
    private String niveau; // DEBUTANT, INTERMEDIAIRE, AVANCE

    @Field("formateur")
    private String formateur;

    @Field("formateur_bio")
    private String formateurBio;

    @Field("lieu")
    private String lieu;

    @Field("lien_visio")
    private String lienVisio;

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

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;


}