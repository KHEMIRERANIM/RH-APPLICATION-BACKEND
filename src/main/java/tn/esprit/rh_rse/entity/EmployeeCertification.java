package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import tn.esprit.rh_rse.entity.enums.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeCertification {

    @Id
    private String id;

    private String evolutionPlanId;
    private String templateId;
    private String nom;

    private CertificationType type;
    private CertificationLevel niveauRequis;

    private Boolean obligatoire; // corrigé

    private EvaluationMethod methodeEval;
    private String organismeUrl;

    @Builder.Default
    private CertificationStatus statut = CertificationStatus.NON_COMMENCE;
    private CertificationLevel niveauObtenu;

    private LocalDate dateObtention;
    private LocalDate datePrevisionnelle;

    private String fichierNom;
    private String fichierUrl;
    private String badgeUrl;

    private String commentaireEmploye;

    private String commentaireAdmin;
    private Boolean valideParAdmin;
}