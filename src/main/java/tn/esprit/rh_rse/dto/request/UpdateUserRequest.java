package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class UpdateUserRequest {
    private String nom;
    private String prenom;
    private String departement;
    private String poste;
    private String managerId;
    private String photoUrl;
    private List<String> regime;      // ajouté
    private List<String> allergies;   // ajouté
}