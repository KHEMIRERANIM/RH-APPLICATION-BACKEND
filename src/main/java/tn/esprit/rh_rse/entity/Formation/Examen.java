package tn.esprit.rh_rse.entity.Formation;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "examens")
public class Examen {

    @Id
    private String id;

    @Field("formation_id")
    private String formationId;

    @Field("titre")
    private String titre;

    @Field("description")
    private String description;

    @Field("duree_minutes")
    private Integer dureeMinutes;

    @Field("date_limite")
    private LocalDateTime dateLimite;

    @Field("questions")
    private List<Question> questions;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}