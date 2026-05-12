package tn.esprit.rh_rse.entity;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "avis")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Avis {

    @Id
    private String id;

    @NotBlank(message = "L'identifiant de l'employé est obligatoire")
    private String userId;

    @NotBlank(message = "L'identifiant du plat est obligatoire")
    private String platId;

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note minimale est 1")
    @Max(value = 5, message = "La note maximale est 5")
    private Integer note;

    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    private String commentaire;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;
}