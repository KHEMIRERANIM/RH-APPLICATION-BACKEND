package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.TypeCarburant;

@Document(collection = "vehicules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicule {

    @Id
    private String id;

    private String employeId;

    private String marque;

    private String modele;

    @Indexed(unique = true)
    private String immatriculation;

    private Integer nbPlaces;

    private TypeCarburant typeCarburant;
}