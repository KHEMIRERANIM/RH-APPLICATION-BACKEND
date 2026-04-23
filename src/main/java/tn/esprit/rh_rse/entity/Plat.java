package tn.esprit.rh_rse.entity;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plat {

    private String platId;

    @NotBlank(message = "Le nom du plat est obligatoire")
    private String nom;

    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @Positive(message = "Le prix doit etre positif")
    private Double prix;

    private List<String> tags;

    private String image;

    @Builder.Default
    private Boolean disponible = true;

    @NotNull(message = "La quantite est obligatoire")
    @Min(value = 0, message = "La quantite ne peut pas etre negative")
    private Integer quantite;

    private String ingredients;

    private Integer calories;
    private Integer proteines;
    private Integer glucides;
    private Integer lipides;
    private Integer sucres;
    private Integer fibres;
    private Integer pctProteines;
    private Integer pctGlucides;
    private Integer pctLipides;
    private Boolean pmrAdapte;
    private String pmrRaison;
    private String niveauCalories;
}
