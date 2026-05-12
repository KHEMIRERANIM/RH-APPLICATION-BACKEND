package tn.esprit.rh_rse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageNotificationResponse {
    private String id;
    private String image;
    private String title;
    private String description;
    private String time;
    private String link;
    private boolean read;
}
