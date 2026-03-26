package tn.esprit.rh_rse.dto.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String nom;
    private String prenom;
    private String telephone;
    private String departement;
    private String poste;
    private String managerId;
    private String photoUrl;
    private String adresse;
    private tn.esprit.rh_rse.entity.enums.Role role;
    private tn.esprit.rh_rse.entity.enums.UserStatus status;
}