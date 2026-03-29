package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import tn.esprit.rh_rse.entity.enums.StatutConge;

@Data
public class ValidationCongeRequest {
    private StatutConge statut;          // APPROUVE ou REFUSE
    private String commentaireManager;
}
