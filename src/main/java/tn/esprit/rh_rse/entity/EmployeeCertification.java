package tn.esprit.rh_rse.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import tn.esprit.rh_rse.entity.enums.*;

import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeCertification {

    @Id
    private String id;

    // 🔥 relation → on stocke l'id seulement
    private String evolutionPlanId;

    private String templateId; // String au lieu de Long (Mongo)

    private String nom;

    private CertificationType type;
    private CertificationLevel niveauRequis;

    private boolean obligatoire;

    private EvaluationMethod methodeEval;

    private String organismeUrl;

    // côté employé
    private CertificationStatus statut = CertificationStatus.NON_COMMENCE;
    private CertificationLevel niveauObtenu;

    private LocalDate dateObtention;
    private LocalDate datePrevisionnelle;

    private String fichierNom;
    private String fichierUrl;
    private String badgeUrl;

    private String commentaireEmploye;

    // admin
    private String commentaireAdmin;
    private Boolean valideParAdmin;
}
