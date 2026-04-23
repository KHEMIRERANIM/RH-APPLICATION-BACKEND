package tn.esprit.rh_rse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public void notifierNouvelleDemande(String employeNom, int nombreJours) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "NOUVELLE_DEMANDE");
        notification.put("message", "📋 Nouvelle demande de congé de " + employeNom + " (" + nombreJours + " jours)");
        notification.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/demandes", notification);
    }

    public void notifierValidationCongé(String employeId, String statut, String commentaire) {
        String email = userRepository.findById(employeId)
                .map(u -> u.getEmail())
                .orElse(employeId);

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "VALIDATION_CONGE");
        notification.put("message", statut.equals("APPROUVE") ?
                "✅ Votre demande de congé a été approuvée" :
                "❌ Votre demande de congé a été refusée");
        notification.put("commentaire", commentaire);
        notification.put("statut", statut);

        messagingTemplate.convertAndSend("/topic/notifications/" + email, notification);
    }
}