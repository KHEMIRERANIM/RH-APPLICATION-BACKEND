package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "formateurs")
public class Formateur {

    @Id
    private String id;

    @Field("user_id")
    private String userId; // Optionnel: lien avec un utilisateur existant

    @Field("nom")
    private String nom;

    @Field("prenom")
    private String prenom;

    @Field("email")
    private String email;

    @Field("telephone")
    private String telephone;

    @Field("specialite")
    private String specialite;

    @Field("bio")
    private String bio;

    @Field("photo")
    private String photo;

    @Field("status")
    private String status = "ACTIF"; // ACTIF, INACTIF

    @Field("formations_assignees")
    private List<String> formationsAssignees; // Liste des IDs des formations

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}