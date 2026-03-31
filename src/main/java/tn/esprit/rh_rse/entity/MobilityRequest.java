package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.MobilityStatus;

import java.time.LocalDateTime;

@Document(collection = "mobility_requests")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MobilityRequest {

    @Id
    private String id;

    // L'employé qui demande
    private String employeeId;      // ref → User.id
    private String employeeName;    // dénormalisé pour affichage

    // Poste actuel
    private String currentCareerTitle;
    private String currentDepartement;

    // Poste cible (ref → Career)
    private String targetCareerId;
    private String targetCareerTitle;
    private String targetDepartement;

    // Motivation
    private String motivationLetter;

    // Statut workflow
    private MobilityStatus status;  // PENDING, APPROVED, REJECTED, ON_HOLD

    // Traitement RH
    private String reviewedBy;      // User.id du RH
    private String reviewComment;
    private LocalDateTime reviewedAt;

    private LocalDateTime requestedAt;
    private LocalDateTime updatedAt;
    private String motivationFileName;
    private String motivationFileBase64;
}