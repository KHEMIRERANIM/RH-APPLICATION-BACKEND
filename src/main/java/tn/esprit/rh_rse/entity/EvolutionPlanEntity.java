package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.PlanStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "evolution_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvolutionPlanEntity {

    @Id
    private String id;

    private String employeeId;
    private String currentCareerId;
    private String targetCareerId;

    private PlanStatus status = PlanStatus.DRAFT;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<String> competencesJson = new ArrayList<>();
    private List<EmployeeCertification> certifications = new ArrayList<>();

    private List<String> formationsRecommandees = new ArrayList<>();
    private String commentaireAdmin;

    private Integer poidsScoreTechnique = 70;
    private Integer poidsScoreSoftSkill = 30;

    // Scores calculés côté backend
    private Integer scoreTechnique = 0;
    private Integer scoreSoftSkill = 0;
    private Integer scoreGlobal = 0;
}