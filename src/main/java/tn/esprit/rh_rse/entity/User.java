package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.entity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String telephone;
    private String adresse;

    private Role role;
    private UserStatus status;
    private String departement;
    private String poste;
    private String managerId;
    private String photoUrl;

    private LocalDateTime dateEmbauche;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<String> regime;
    private List<String> allergies;
}