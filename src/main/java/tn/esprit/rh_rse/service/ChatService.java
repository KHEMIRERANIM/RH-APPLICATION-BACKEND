package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.dto.response.MessageNotificationResponse;
import tn.esprit.rh_rse.dto.response.UserResponse;
import tn.esprit.rh_rse.entity.Message;
import tn.esprit.rh_rse.entity.User;
import tn.esprit.rh_rse.entity.enums.Role;
import tn.esprit.rh_rse.repository.MessageRepository;
import tn.esprit.rh_rse.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<UserResponse> getContacts() {
        List<User> contacts = userRepository.findByRoleNot(Role.CANDIDAT);
        return contacts.stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

    public List<Message> getConversation(String currentUserId, String contactId) {
        List<Message> messages = messageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByCreatedAtAsc(
                currentUserId, contactId, currentUserId, contactId
        );
        // Marquer comme lus les messages recus
        messages.stream()
                .filter(m -> m.getReceiverId().equals(currentUserId) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });
        return messages;
    }

    public Message sendMessage(String senderId, String receiverId, String content) {
        Message message = Message.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .build();
        Message savedMessage = messageRepository.save(message);

        // Notify via WebSocket
        User sender = userRepository.findById(senderId).orElse(null);
        if (sender != null) {
            MessageNotificationResponse notification = mapToNotification(savedMessage, sender);
            messagingTemplate.convertAndSend("/topic/messages/" + receiverId, notification);
        }

        return savedMessage;
    }

    public List<MessageNotificationResponse> getUnreadMessages(String userId) {
        List<Message> unread = messageRepository.findByReceiverIdAndReadFalse(userId);
        return unread.stream()
                .map(m -> {
                    User sender = userRepository.findById(m.getSenderId()).orElse(null);
                    return mapToNotification(m, sender);
                })
                .collect(Collectors.toList());
    }

    public void markMessageAsRead(String messageId) {
        Optional<Message> msgOpt = messageRepository.findById(messageId);
        if (msgOpt.isPresent()) {
            Message msg = msgOpt.get();
            msg.setRead(true);
            messageRepository.save(msg);
        }
    }

    private MessageNotificationResponse mapToNotification(Message message, User sender) {
        String title = sender != null ? sender.getPrenom() + " " + sender.getNom() : "Utilisateur inconnu";
        // Ne pas utiliser d'image par défaut si l'utilisateur n'en a pas
        String image = (sender != null && sender.getPhotoUrl() != null && !sender.getPhotoUrl().isEmpty())
                ? sender.getPhotoUrl()
                : null;

        return MessageNotificationResponse.builder()
                .id(message.getId())
                .title(title)
                .description(message.getContent())
                .time(message.getCreatedAt().toString())
                .image(image)
                .read(message.isRead())
                // Route correcte : /apps/chat/{senderId} pour ouvrir la conversation
                .link("/apps/chat/" + message.getSenderId())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .telephone(user.getTelephone())
                .role(user.getRole())
                .status(user.getStatus())
                .departement(user.getDepartement())
                .poste(user.getPoste())
                .photoUrl(user.getPhotoUrl())
                .build();
    }
}
