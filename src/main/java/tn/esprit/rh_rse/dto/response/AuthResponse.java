package tn.esprit.rh_rse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import tn.esprit.rh_rse.entity.enums.Role;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private Role role;
    private String poste;

}