package tn.esprit.rh_rse.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.CategorieTransport;
import tn.esprit.rh_rse.entity.enums.StatutTrajet;

import java.time.LocalDate;
import java.time.LocalTime;

@Document(collection = "trajets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trajet {

    @Id
    private String id;

    private String employeId;

    private String vehiculeId;

    private CategorieTransport categorie;

    private String adresseDepart;

    private String adresseArrivee;

    private LocalTime heureDepart;

    private String joursDisponibles;

    private Integer placesDisponibles;

    private Integer placesRestantes;

    private StatutTrajet statut;
    
    private Double prix;

    private LocalDate dateCreation;
}