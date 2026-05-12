package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rh_rse.dto.request.SendMessageRequest;
import tn.esprit.rh_rse.dto.response.UserResponse;
import tn.esprit.rh_rse.entity.Message;
import tn.esprit.rh_rse.service.ChatService;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/contacts")
    public ResponseEntity<List<UserResponse>> getContacts() {
        return ResponseEntity.ok(chatService.getContacts());
    }

    @GetMapping("/messages/{currentUserId}/{contactId}")
    public ResponseEntity<List<Message>> getConversation(
            @PathVariable("currentUserId") String currentUserId,
            @PathVariable("contactId") String contactId) {
        return ResponseEntity.ok(chatService.getConversation(currentUserId, contactId));
    }

    @PostMapping("/messages")
    public ResponseEntity<Message> sendMessage(@RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(request.getSenderId(), request.getReceiverId(), request.getContent()));
    }

    @GetMapping("/messages/unread/{userId}")
    public ResponseEntity<List<tn.esprit.rh_rse.dto.response.MessageNotificationResponse>> getUnreadMessages(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(chatService.getUnreadMessages(userId));
    }

    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<Void> markMessageAsRead(@PathVariable("messageId") String messageId) {
        chatService.markMessageAsRead(messageId);
        return ResponseEntity.ok().build();
    }
}
