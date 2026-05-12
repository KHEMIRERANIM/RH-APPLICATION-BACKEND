package tn.esprit.rh_rse.dto.request;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String senderId;
    private String receiverId;
    private String content;
}
