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
    @Positive(message = "Le prix doit être positif")
    private Double prix;

    private List<String> tags;  // ["diabetique", "sans_gluten"]

    private String image;

    private Boolean disponible = true;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 0, message = "La quantité ne peut pas être négative")
    private Integer quantite;
}