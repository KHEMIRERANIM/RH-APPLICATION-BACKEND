package tn.esprit.rh_rse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RseUserDataResponse {
    private String id;
    private String nom;
    private String prenom;
    private String email;
    private int rsePoints;
    private String rseLevel;
    private List<String> badges;
}