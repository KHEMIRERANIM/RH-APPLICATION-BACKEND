package tn.esprit.rh_rse.dto.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String nom;
    private String prenom;
    private String departement;
    private String poste;
    private String managerId;
    private String photoUrl;
}