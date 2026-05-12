// MotifAnnulationDto.java
package tn.esprit.rh_rse.dto.Formation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotifAnnulationDto {

    @NotBlank(message = "La raison est obligatoire")
    @Size(min = 5, max = 500, message = "La raison doit contenir entre 5 et 500 caractères")
    private String raison;

    private TypeMotif typeMotif;

    public enum TypeMotif {
        CONFLIT_HORAIRE("Conflit d'horaire"),
        MALADIE("Raison médicale"),
        URGENCE("Urgence personnelle"),
        AUTRE("Autre raison");

        private String libelle;

        TypeMotif(String libelle) {
            this.libelle = libelle;
        }

        public String getLibelle() {
            return libelle;
        }
    }
}