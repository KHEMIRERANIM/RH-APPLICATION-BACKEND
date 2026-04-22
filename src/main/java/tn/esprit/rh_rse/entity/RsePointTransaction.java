package tn.esprit.rh_rse.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "rse_point_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RsePointTransaction {

    @Id
    private String id;

    private int points;
    private String reason;

    private User user;

    private LocalDateTime createdAt;
}