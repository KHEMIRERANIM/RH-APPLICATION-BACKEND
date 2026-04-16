package tn.esprit.rh_rse.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @NotBlank(message = "L'identifiant du menu est obligatoire")
    private String menuId;

    // platId → quantité choisie (ex: {"abc": 2, "def": 1})
    private java.util.Map<String, Integer> platsQuantites;

    // Garde la liste des platIds pour compatibilité
    private List<String> plats;

    private LocalDate dateCommande;
    @JsonProperty("datePrete")
    private LocalDateTime datePrete;

    @Pattern(regexp = "en_attente|confirmee|prete|livree", message = "Statut invalide")
    private String statut = "en_attente";

    @PositiveOrZero
    private Double montantTotal;

    @PositiveOrZero
    private Double montantBrut;          // total AVANT réduction

    private String codeRetrait;
    private boolean reductionAppliquee = false;
    private double montantReduction     = 0.0;
}