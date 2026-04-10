package tn.esprit.rh_rse.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlternativeDTO {
    private String id;
    private String type;
    private String adresseDepart;
    private String adresseArrivee;
    private String heureDepart;
    private int placesRestantes;
    private int priorite;
    private String conducteurNom;
    private Double prix;
}