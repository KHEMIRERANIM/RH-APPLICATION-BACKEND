package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "rse_actions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RseAction {

    @Id
    private String id;

    private String type;
    private String description;
    private String status;

    private User employee;

    private LocalDateTime createdAt;
}