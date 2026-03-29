package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import tn.esprit.rh_rse.entity.enums.Role;

@Data
public class CreateUserRequest {
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private Role role;
    private String departement;
    private String poste;
    private String managerId;
    private String photoUrl;
}