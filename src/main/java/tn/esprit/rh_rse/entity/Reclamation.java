package tn.esprit.rh_rse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reclamations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reclamation {

    @Id
    private String id;
    private String employeId;
    private String busId;
    private String stopName;
    private String neighborhood;
    private Double latitude;
    private Double longitude;
    private LocalDateTime date;
    private String status; // e.g., "PENDING", "RESOLVED"
}
