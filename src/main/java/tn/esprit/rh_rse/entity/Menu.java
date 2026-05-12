package tn.esprit.rh_rse.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "menus")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Menu {

    @Id
    private String id;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 100, message = "Le titre doit avoir entre 3 et 100 caractères")
    private String titre;

    @NotBlank(message = "Le statut est obligatoire")
    @Pattern(regexp = "brouillon|publie", message = "Statut invalide : brouillon ou publie")
    private String statut;

    @Valid
    private List<Plat> plats = new ArrayList<>();
}