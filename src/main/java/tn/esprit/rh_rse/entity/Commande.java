package tn.esprit.rh_rse.entity;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Document(collection = "commandes")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Commande {

    @Id
    private String id;

    @NotBlank(message = "L'identifiant de l'employé est obligatoire")
    private String userId;

    @NotBlank(message = "L'identifiant du menu est obligatoire")
    private String menuId;

    @NotEmpty(message = "La commande doit contenir au moins un plat")
    private List<String> plats;

    //  Plus de @NotNull — le service set la date automatiquement
    private LocalDate dateCommande;

    @Pattern(regexp = "en_attente|confirmee|prete|livree",
            message = "Statut invalide")
    private String statut = "en_attente";

    @PositiveOrZero(message = "Le montant total ne peut pas être négatif")
    private Double montantTotal;
}