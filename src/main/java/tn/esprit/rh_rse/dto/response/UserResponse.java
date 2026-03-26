package tn.esprit.rh_rse.dto.response;

import lombok.Builder;
import lombok.Data;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.entity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private String id;
    private String nom;
    private String prenom;
    private String telephone;
    private String email;
    private Role role;
    private UserStatus status;
    private String departement;
    private String poste;
    private String managerId;
    private String photoUrl;
    private String adresse;
    private LocalDateTime dateEmbauche;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> regime;
    private List<String> allergies;
}