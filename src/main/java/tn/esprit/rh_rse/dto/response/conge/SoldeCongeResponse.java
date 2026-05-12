package tn.esprit.rh_rse.dto.response.conge;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SoldeCongeResponse {
    private String employeId;
    private int annee;
    private int joursTotal;
    private int joursUtilises;
    private int joursEnAttente;
    private int joursRestants;
}
