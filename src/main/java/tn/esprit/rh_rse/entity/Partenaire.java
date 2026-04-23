package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.CategorieOffreAvantage;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "partenaires")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partenaire {

    @Id
    private String id;

    private String nom;
    private CategorieOffreAvantage type;
    private String logoUrl;
    private String emailContact;
    private LocalDate dateConvention;
    private boolean actif;
    private LocalDateTime createdAt;
}