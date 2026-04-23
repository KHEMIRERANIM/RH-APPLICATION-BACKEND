package tn.esprit.rh_rse.entity;

import lombok.*;
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

    private String idUser;

    private TypeNotification type;

    private String idOffreAvantage;

    private String titreOffreAvantage;

    private String message;

    @Builder.Default
    private boolean lu = false;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
