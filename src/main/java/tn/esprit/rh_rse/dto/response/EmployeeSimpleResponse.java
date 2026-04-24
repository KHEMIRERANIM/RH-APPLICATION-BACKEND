package tn.esprit.rh_rse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import tn.esprit.rh_rse.entity.User;

@Data
@AllArgsConstructor
public class EmployeeSimpleResponse {
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String departement;
    private String poste;
    private String photoUrl;
    private String status;

    public static EmployeeSimpleResponse fromUser(User u) {
        return new EmployeeSimpleResponse(
                u.getId(),
                u.getNom(),
                u.getPrenom(),
                u.getEmail(),
                u.getTelephone(),
                u.getDepartement(),
                u.getPoste(),
                u.getPhotoUrl(),
                u.getStatus() != null ? u.getStatus().name() : null
        );
    }
}