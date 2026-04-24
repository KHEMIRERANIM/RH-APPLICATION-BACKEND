// entity/Formation/FraudEvent.java
package tn.esprit.rh_rse.entity.Formation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fraud_events")
public class FraudEvent {

    @Id
    private String id;

    @Indexed
    @Field("examenId")
    private String examenId;

    @Indexed
    @Field("employeId")
    private String employeId;

    @Field("employeNom")
    private String employeNom;

    @Field("employePrenom")
    private String employePrenom;

    @Field("employeEmail")
    private String employeEmail;

    @Field("eventType")
    private String eventType;

    @Field("timestamp")
    private Long timestamp;

    @Field("description")
    private String description;

    @Field("riskScore")
    private Integer riskScore;

    @Field("isBlocked")
    private Boolean isBlocked;

    @CreatedDate
    @Field("createdAt")
    private LocalDateTime createdAt;

    // Constructeur simplifié
    public FraudEvent(String examenId, String employeId, String eventType, Long timestamp, String description) {
        this.examenId = examenId;
        this.employeId = employeId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.isBlocked = false;
        this.riskScore = calculateRiskScore(eventType);
    }

    public FraudEvent(String examenId, String employeId, String employeNom, String employePrenom,
                      String employeEmail, String eventType, Long timestamp, String description) {
        this.examenId = examenId;
        this.employeId = employeId;
        this.employeNom = employeNom;
        this.employePrenom = employePrenom;
        this.employeEmail = employeEmail;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.isBlocked = false;
        this.riskScore = calculateRiskScore(eventType);
    }

    private Integer calculateRiskScore(String eventType) {
        switch (eventType) {
            case "TAB_SWITCH": return 30;
            case "WINDOW_BLUR": return 25;
            case "VISIBILITY_HIDDEN": return 35;
            case "FOCUS_LOST": return 20;
            default: return 10;
        }
    }
}