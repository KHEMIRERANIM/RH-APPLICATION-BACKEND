package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.entity.enums.UserStatus;

@Data
public class CreateUserRequest {
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
}