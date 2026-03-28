package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.RoleTrajet;
import tn.esprit.rh_rse.entity.enums.TypeCarburant;

import java.time.LocalDate;

@Document(collection = "empreintes_carbone")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpreinteCarbone {

    @Id
    private String id;

    private String trajetId;

    private String employeId;

    private RoleTrajet role;

    private Double distanceKm;

    private Integer nbPassagers;

    private TypeCarburant typeCarburant;

    private Double co2EconomiseKg;

    private Integer pointsEco;

    private LocalDate dateCalcul;
}