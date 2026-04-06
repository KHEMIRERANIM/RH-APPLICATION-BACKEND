package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.CertificationLevel;
import tn.esprit.rh_rse.entity.enums.CertificationType;
import tn.esprit.rh_rse.entity.enums.EvaluationMethod;

@Document(collection = "certification_templates")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CertificationTemplate {

    @Id
    private String id;

    // MongoDB ne gère pas les relations comme JPA
    private String careerId; // on stocke juste l'id

    private String nom;

    private CertificationType type;

    private CertificationLevel niveauRequis;

    private boolean obligatoire;

    private String organismeUrl;

    private EvaluationMethod methodeEval;

    private String description;
}