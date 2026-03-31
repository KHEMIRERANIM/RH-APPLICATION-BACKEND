package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.PlanStatus;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "career_plans")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CareerPlan {

    @Id
    private String id;

    private String employeeId;
    private String employeeName;

    // Poste actuel
    private String currentCareerId;
    private String currentCareerTitle;
    private List<String> currentSkills;   // skills que l'employé possède déjà

    // Poste cible
    private String targetCareerId;
    private String targetCareerTitle;
    private List<String> targetSkills;    // skills requises par le poste cible

    // Gap calculé automatiquement
    private List<String> skillsToAcquire; // targetSkills - currentSkills
    private List<String> skillsAlreadyMet; // intersection

    // Progression
    private int progressPercent;          // calculé : skillsAlreadyMet / targetSkills * 100
    private PlanStatus status;            // ACTIVE, COMPLETED, CANCELLED

    // RH
    private String createdBy;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}