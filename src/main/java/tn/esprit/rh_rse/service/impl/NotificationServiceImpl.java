package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.NotificationRepository;
import tn.esprit.rh_rse.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // --- HEAD (Mutuelle) Methods ---

    @Override
    public List<Notification> getMesNotifications(String idUser) {
        return notificationRepository.findByIdUserOrderByDateCreationDesc(idUser);
    }

    @Override
    public Notification marquerCommeLu(String idNotification, String idUser) {
        Notification notification = notificationRepository.findById(idNotification)
                .orElseThrow(() -> new RuntimeException("Notification introuvable"));
        
        if (notification.getIdUser() != null && !notification.getIdUser().equals(idUser)) {
            throw new RuntimeException("Non autorisé");
        }
        
        notification.setLu(true);
        return notificationRepository.save(notification);
    }

    @Override
    public void marquerToutCommeLu(String idUser) {
        List<Notification> notifications = notificationRepository.findByIdUserOrderByDateCreationDesc(idUser);
        for (Notification notif : notifications) {
            if (!notif.isLu()) {
                notif.setLu(true);
            }
        }
        notificationRepository.saveAll(notifications);
    }

    @Override
    public long getNbNonLues(String idUser) {
        return notificationRepository.countByIdUserAndLuFalse(idUser);
    }

    @Override
    public void supprimerNotification(String idNotification, String idUser) {
        Notification notification = notificationRepository.findById(idNotification)
                .orElseThrow(() -> new RuntimeException("Notification introuvable"));
        
        if (notification.getIdUser() != null && !notification.getIdUser().equals(idUser)) {
            throw new RuntimeException("Non autorisé");
        }
        
        notificationRepository.delete(notification);
    }

    // --- transport Methods ---

    @Override
    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }

    @Override
    public Notification getById(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée : " + id));
    }

    @Override
    public Notification create(Notification notification) {
        notification.setLu(false);
        notification.setDateCreation(LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);

        // Envoi WebSocket en temps réel
        envoyerWebSocket(saved);

        return saved;
    }

    @Override
    public void delete(String id) {
        notificationRepository.deleteById(id);
    }

    @Override
    public List<Notification> getByDestinataire(String destinataireId) {
        return notificationRepository.findByDestinataireId(destinataireId);
    }

    @Override
    public List<Notification> getNonLues(String destinataireId) {
        return notificationRepository.findByDestinataireIdAndLu(destinataireId, false);
    }

    @Override
    public Notification marquerCommeLu(String id) {
        Notification notification = getById(id);
        notification.setLu(true);
        return notificationRepository.save(notification);
    }

    @Override
    public void marquerToutesCommeLues(String destinataireId) {
        List<Notification> nonLues = notificationRepository
                .findByDestinataireIdAndLu(destinataireId, false);
        nonLues.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(nonLues);
    }

    @Override
    public long countNonLues(String destinataireId) {
        return notificationRepository.countByDestinataireIdAndLu(destinataireId, false);
    }

    @Override
    public Notification envoyerNotification(String destinataireId, String expediteurId,
                                            String trajetId, TypeNotification type,
                                            String contenu) {
        Notification notification = Notification.builder()
                .destinataireId(destinataireId)
                .expediteurId(expediteurId)
                .trajetId(trajetId)
                .type(type)
                .contenu(contenu)
                .lu(false)
                .dateCreation(LocalDateTime.now())
                .build();
        Notification saved = notificationRepository.save(notification);

        // Envoi WebSocket en temps réel
        envoyerWebSocket(saved);

        return saved;
    }

    @Override
    public Notification envoyerDemandeConfirmation(String conducteurId, String passagerId,
                                                   String trajetId, String reservationId) {
        Notification notification = Notification.builder()
                .destinataireId(conducteurId)
                .expediteurId(passagerId)
                .trajetId(trajetId)
                .reservationId(reservationId)
                .type(TypeNotification.DEMANDE_CONFIRMATION)
                .contenu("Un employé demande à rejoindre votre trajet. Confirmez ou refusez.")
                .lu(false)
                .dateCreation(LocalDateTime.now())
                .build();
        Notification saved = notificationRepository.save(notification);

        // Envoi WebSocket en temps réel
        envoyerWebSocket(saved);

        return saved;
    }

    // WebSocket Helper
    private void envoyerWebSocket(Notification notification) {
        try {
            String targetId = notification.getDestinataireId() != null ? 
                             notification.getDestinataireId() : 
                             notification.getIdUser();
            if (targetId != null) {
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + targetId,
                        notification
                );
            }
        } catch (Exception e) {
            System.err.println("WebSocket non disponible pour: " + notification.getDestinataireId());
        }
    }
}
