package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "paiements")
public class PaiementPlat {

    @Id
    private String id;

    private String commandeId;
    private String userId;
    private String nomEmploye;

    private double montantBrut;
    private double montantReduction;
    private double montantNet;

    private boolean reductionAppliquee;
    private int pointsGagnes;

    // "especes" ou "salaire"
    private String modePaiement;

    // "paye" ou "en_attente_integration" (pour déduction salaire)
    private String statut;

    private LocalDateTime datePaiement;
}