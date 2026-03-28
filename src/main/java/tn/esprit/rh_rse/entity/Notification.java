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

    private String destinataireId;

    private String expediteurId;

    private String trajetId;

    private String reservationId;

    private TypeNotification type;

    private String contenu;

    private Boolean lu = false;

    private LocalDateTime dateCreation;
}