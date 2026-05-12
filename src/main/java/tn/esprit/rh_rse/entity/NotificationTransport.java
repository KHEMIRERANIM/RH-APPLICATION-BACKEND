package tn.esprit.rh_rse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import tn.esprit.rh_rse.entity.enums.TypeNotification;

import java.time.LocalDateTime;

@Document(collection = "notification_transports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTransport {

    @Id
    private String id;

    private String destinataireId;
    private String expediteurId;
    private String trajetId;
    private String reservationId;
    private String contenu;

    private TypeNotification type;

    @Builder.Default
    private boolean lu = false;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
