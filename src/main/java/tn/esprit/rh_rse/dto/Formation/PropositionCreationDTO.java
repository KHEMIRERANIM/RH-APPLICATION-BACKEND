// dto/Formation/PropositionCreationDTO.java
package tn.esprit.rh_rse.dto.Formation;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropositionCreationDTO {

    @NotBlank(message = "La technologie est requise")
    private String technologie;

    @NotBlank(message = "Le titre est requis")
    @Size(min = 5, max = 200, message = "Le titre doit contenir entre 5 et 200 caractères")
    private String titre;

    @NotBlank(message = "La description est requise")
    @Size(min = 20, max = 1000, message = "La description doit contenir entre 20 et 1000 caractères")
    private String description;

    private String objectifs;

    @Min(value = 1, message = "La durée minimum est de 1 heure")
    @Max(value = 100, message = "La durée maximum est de 100 heures")
    private Integer dureeHeures = 14;

    @NotBlank(message = "Le niveau est requis")
    @Pattern(regexp = "DEBUTANT|INTERMEDIAIRE|EXPERT", message = "Niveau invalide")
    private String niveau = "INTERMEDIAIRE";

    private String publicCible;
    private String preRequis;

    @NotBlank(message = "Le type est requis")
    @Pattern(regexp = "TECHNIQUE|MANAGERIAL|RSE|SOFT_SKILLS", message = "Type invalide")
    private String type = "TECHNIQUE";

    private String source = "IA_RECOMMENDATION";
    private Double scoreIA;

    private String employeId;
    private String employeNom;
}