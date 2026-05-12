// dto/Formation/PropositionValidationDTO.java
package tn.esprit.rh_rse.dto.Formation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropositionValidationDTO {

    @NotBlank(message = "L'ID de l'admin est requis")
    private String adminId;

    private String commentaire;

    @NotBlank(message = "L'action est requise")
    private String action; // VALIDER, REJETER
}