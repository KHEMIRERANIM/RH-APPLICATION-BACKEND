package tn.esprit.rh_rse.entity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.PlanStatus;

import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "evolution_plans")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EvolutionPlanEntity {

    @Id
    private String id;

    // 🔥 relations remplacées par IDs
    private String employeeId;
    private String currentCareerId;
    private String targetCareerId;

    private PlanStatus status = PlanStatus.DRAFT;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ✔ Mongo = stockage direct
    private List<String> competencesJson = new ArrayList<>();

    // ✔ imbriqué (plus simple en Mongo)
    private List<EmployeeCertification> certifications = new ArrayList<>();

    // admin
    private List<String> formationsRecommandees = new ArrayList<>();

    private String commentaireAdmin;

    private Integer poidsScoreTechnique = 70;
    private Integer poidsScoreSoftSkill = 30;
}