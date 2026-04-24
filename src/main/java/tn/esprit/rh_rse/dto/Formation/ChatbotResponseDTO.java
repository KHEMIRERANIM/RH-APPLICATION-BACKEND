package tn.esprit.rh_rse.dto.Formation;
// dto/Chatbot/ChatbotResponseDTO.java

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChatbotResponseDTO {
    private String answer;
    private List<String> sources;
    private boolean success;
    private String errorMessage;
}