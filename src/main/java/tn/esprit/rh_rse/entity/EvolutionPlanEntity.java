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

    @Builder.Default
    private PlanStatus status = PlanStatus.DRAFT;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<String> competencesJson = new ArrayList<>();
    @Builder.Default
    private List<EmployeeCertification> certifications = new ArrayList<>();

    @Builder.Default
    private List<String> formationsRecommandees = new ArrayList<>();
    private String commentaireAdmin;

    @Builder.Default
    private Integer poidsScoreTechnique = 70;
    @Builder.Default
    private Integer poidsScoreSoftSkill = 30;

    // Scores calculés côté backend
    @Builder.Default
    private Integer scoreTechnique = 0;
    @Builder.Default
    private Integer scoreSoftSkill = 0;
    @Builder.Default
    private Integer scoreGlobal = 0;
}