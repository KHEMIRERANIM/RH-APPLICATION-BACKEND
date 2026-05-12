// dto/Formation/PropositionRejetDTO.java
package tn.esprit.rh_rse.dto.Formation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropositionRejetDTO {

    @NotBlank(message = "L'ID de l'admin est requis")
    private String adminId;

    @NotBlank(message = "La raison du rejet est requise")
    private String raison;

    private String commentaireSupplementaire;
}