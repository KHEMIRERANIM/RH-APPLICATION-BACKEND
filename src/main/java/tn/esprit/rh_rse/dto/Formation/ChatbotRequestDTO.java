// ChatbotRequestDTO.java
package tn.esprit.rh_rse.dto.Formation;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatbotRequestDTO {

    @NotBlank(message = "La question ne peut pas être vide")
    private String question;

    private String sessionId; // Optionnel, si non fourni on utilise la session HTTP
}