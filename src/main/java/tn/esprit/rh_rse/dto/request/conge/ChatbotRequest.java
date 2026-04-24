package tn.esprit.rh_rse.dto.request.conge;

import lombok.Data;

import java.util.List;

@Data
public class ChatbotRequest {
    private String message;
    private String employeId;
    private List<Message> historique;

    @Data
    public static class Message {
        private String role; // "user" ou "assistant"
        private String content;
    }
}
