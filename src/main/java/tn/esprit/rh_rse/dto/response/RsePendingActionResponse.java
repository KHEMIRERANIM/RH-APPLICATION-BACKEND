package tn.esprit.rh_rse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RsePendingActionResponse {
    private String id;
    private String employeeId;
    private String nom;
    private String prenom;
    private String email;
    private String photoUrl;
    private String type;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    private int rsePoints;
    private String rseLevel;
    private List<String> badges;
}