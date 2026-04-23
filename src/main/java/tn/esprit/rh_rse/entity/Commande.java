package tn.esprit.rh_rse.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "commandes")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Commande {

    @Id
    private String id;

    @NotBlank(message = "L'identifiant de l'employé est obligatoire")
    private String userId;

    private String nomEmploye;

    @NotBlank(message = "L'identifiant du menu est obligatoire")
    private String menuId;

    private java.util.Map<String, Integer> platsQuantites;
    private List<String> plats;

    private LocalDate dateCommande;

    @JsonProperty("datePrete")
    private LocalDateTime datePrete;

    @Builder.Default
    @Pattern(regexp = "en_attente|confirmee|prete|livree", message = "Statut invalide")
    private String statut = "en_attente";

    @PositiveOrZero
    private Double montantTotal;

    @PositiveOrZero
    private Double montantBrut;

    private String codeRetrait;
    @Builder.Default
    private boolean reductionAppliquee = false;
    @Builder.Default
    private double montantReduction = 0.0;
}