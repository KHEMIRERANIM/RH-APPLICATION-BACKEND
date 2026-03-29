package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import tn.esprit.rh_rse.entity.enums.TypeConge;

import java.time.LocalDate;

@Data
public class DemandeCongeRequest {
    private String employeId;
    private String managerId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String motif;
    private TypeConge type;
}
