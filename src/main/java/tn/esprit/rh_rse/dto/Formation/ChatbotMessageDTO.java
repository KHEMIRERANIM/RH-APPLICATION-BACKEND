// dto/ChatbotMessageDTO.java
package tn.esprit.rh_rse.dto.Formation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageDTO {
    private String id;
    private String content;
    private String sender; // "USER" ou "BOT"
    private LocalDateTime timestamp;
    private String sessionId;
}