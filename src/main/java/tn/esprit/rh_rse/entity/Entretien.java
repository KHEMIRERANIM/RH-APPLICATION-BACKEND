package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.StatutEntretien;
import tn.esprit.rh_rse.entity.enums.TypeEntretien;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "entretiens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entretien {

    @Id
    private String id;

    private String candidatureId;
    private String recruteurId;

    private TypeEntretien type;
    private LocalDateTime dateHeure;
    private Integer dureeMinutes;

    private String lieu;
    private String lienVisio;

    private StatutEntretien statut;

    // Candidat-side confirmation (for candidate calendar visibility)
    private boolean confirmeParCandidat;
    private LocalDateTime dateConfirmationCandidat;

    private String feedbackGlobal;
    private Integer noteGlobale;
    private List<String> pointsForts;
    private List<String> pointsFaibles;
    private boolean recommandeEmbauche;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}