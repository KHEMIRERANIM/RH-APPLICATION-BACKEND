package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications_carriere")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotifCarriere {

    @Id
    private String id;

    private String userId;        // destinataire
    private String title;
    private String message;
    private String type;          // MOBILITY_APPROVED, CERTIF_VALIDATED, etc.
    private String link;          // route Angular optionnelle
    private boolean read = false;
    private LocalDateTime createdAt;
}