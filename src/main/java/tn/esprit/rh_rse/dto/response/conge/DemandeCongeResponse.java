package tn.esprit.rh_rse.dto.response.conge;

import lombok.Builder;
import lombok.Data;
import tn.esprit.rh_rse.entity.enums.StatutConge;
import tn.esprit.rh_rse.entity.enums.TypeConge;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DemandeCongeResponse {
    private String id;
    private String employeId;
    private String managerId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int nombreJours;
    private String motif;
    private TypeConge type;
    private StatutConge statut;
    private String commentaireManager;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
