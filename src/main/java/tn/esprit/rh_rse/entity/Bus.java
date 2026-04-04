package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;
import tn.esprit.rh_rse.entity.enums.TypeCarburant;

import java.time.LocalDate;

@Document(collection = "bus")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bus {

    @Id
    private String id;

    private String marque;
    private String modele;
    private String immatriculation;
    private Integer capacite;
    private TypeCarburant typeCarburant;
    private String ligne;              // Ex: "SiegeA-LAC"
    private String heureDepart;
    private Integer dureeMinutes;
    private String joursDisponibles;
    private Integer placesRestantes;
    private StatutTrajet statut;
    private LocalDate dateCreation;
    private String photoUrl;  // Ajoutez cette ligne

    /** Identifiant du pack (bus créés ensemble : actifs + réserve inactifs). */
    private String packId;

}