package tn.esprit.rh_rse.dto.request;

import lombok.Data;
import tn.esprit.rh_rse.entity.enums.TypeEntretien;

import java.time.LocalDateTime;

@Data
public class CreateEntretienRequest {
    private String candidatureId;
    private String recruteurId;
    private TypeEntretien type;
    private LocalDateTime dateHeure;
    private Integer dureeMinutes;
    private String lieu;
    private String lienVisio;
}