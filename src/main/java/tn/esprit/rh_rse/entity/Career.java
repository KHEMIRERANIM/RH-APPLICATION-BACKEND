package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.CareerLevel;
import tn.esprit.rh_rse.entity.enums.CareerDomain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "careers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Career {

    @Id
    private String id;

    private String title;
    private String description;
    private CareerLevel level;
    private CareerDomain domain;
    private List<String> requiredSkills;
    private String departement;
    private String poste;
    private Double salaryMin;
    private Double salaryMax;
    private Boolean isRemoteFriendly;
    private Boolean isAccessibleForDisabled;

    private String userId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EmployeeCertification> certifRequises = new ArrayList<>();
}