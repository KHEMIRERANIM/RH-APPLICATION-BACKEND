package tn.esprit.rh_rse.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private String employeId;
    private String titre;
    private String message;
    private String type;
    private String trajetAnnuleId;
    private String reservationId;
}