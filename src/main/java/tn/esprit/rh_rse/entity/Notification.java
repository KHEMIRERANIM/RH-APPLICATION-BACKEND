package tn.esprit.rh_rse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.TypeNotification;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    // From Mutuelle branch
    private String idUser;
    private String idOffreAvantage;
    private String titreOffreAvantage;
    private String message;

    // From Transport branch
    private String destinataireId;
    private String expediteurId;
    private String trajetId;
    private String reservationId;
    private String contenu;

    // Common
    private TypeNotification type;

    @Builder.Default
    private boolean lu = false;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
