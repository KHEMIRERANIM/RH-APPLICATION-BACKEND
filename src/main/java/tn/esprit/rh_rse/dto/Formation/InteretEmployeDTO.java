// dto/Formation/InteretEmployeDTO.java
package tn.esprit.rh_rse.dto.Formation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteretEmployeDTO {

    @NotBlank(message = "L'ID de l'employé est requis")
    private String employeId;

    @NotBlank(message = "Le nom de l'employé est requis")
    private String employeNom;

    @NotBlank(message = "L'email de l'employé est requis")
    private String employeEmail;
}