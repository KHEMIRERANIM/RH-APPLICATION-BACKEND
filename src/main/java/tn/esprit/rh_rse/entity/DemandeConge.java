package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.StatutConge;
import tn.esprit.rh_rse.entity.enums.TypeConge;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "demandes_conge")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeConge {

    @Id
    private String id;

    private String employeId;        // ID de l'employé qui demande
    private String managerId;        // ID du manager responsable

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int nombreJours;         // calculé automatiquement

    private String motif;
    private TypeConge type;

    @Builder.Default
    private StatutConge statut = StatutConge.EN_ATTENTE;

    private String commentaireManager;  // Commentaire du manager lors validation/refus

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
