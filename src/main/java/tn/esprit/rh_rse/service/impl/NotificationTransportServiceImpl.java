package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.NotificationTransport;
import tn.esprit.rh_rse.entity.enums.TypeNotification;
import tn.esprit.rh_rse.repository.NotificationTransportRepository;
import tn.esprit.rh_rse.service.NotificationTransportService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationTransportServiceImpl implements NotificationTransportService {

    private final NotificationTransportRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public List<NotificationTransport> getAll() {
        return notificationRepository.findAll();
    }

    @Override
    public NotificationTransport getById(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée : " + id));
    }

    @Override
    public NotificationTransport create(NotificationTransport notification) {
        notification.setLu(false);
        notification.setDateCreation(LocalDateTime.now());
        NotificationTransport saved = notificationRepository.save(notification);
        envoyerWebSocket(saved);
        return saved;
    }

    @Override
    public void delete(String id) {
        notificationRepository.deleteById(id);
    }

    @Override
    public List<NotificationTransport> getByDestinataire(String destinataireId) {
        return notificationRepository.findByDestinataireId(destinataireId);
    }

    @Override
    public List<NotificationTransport> getNonLues(String destinataireId) {
        return notificationRepository.findByDestinataireIdAndLu(destinataireId, false);
    }

    @Override
    public NotificationTransport marquerCommeLu(String id) {
        NotificationTransport notification = getById(id);
        notification.setLu(true);
        return notificationRepository.save(notification);
    }

    @Override
    public void marquerToutesCommeLues(String destinataireId) {
        List<NotificationTransport> nonLues = notificationRepository
                .findByDestinataireIdAndLu(destinataireId, false);
        nonLues.forEach(n -> n.setLu(true));
        notificationRepository.saveAll(nonLues);
    }

    @Override
    public long countNonLues(String destinataireId) {
        return notificationRepository.countByDestinataireIdAndLu(destinataireId, false);
    }

    @Override
    public NotificationTransport envoyerNotification(String destinataireId, String expediteurId,
                                            String trajetId, TypeNotification type,
                                            String contenu) {
        NotificationTransport notification = NotificationTransport.builder()
                .destinataireId(destinataireId)
                .expediteurId(expediteurId)
                .trajetId(trajetId)
                .type(type)
                .contenu(contenu)
                .lu(false)
                .dateCreation(LocalDateTime.now())
                .build();
        NotificationTransport saved = notificationRepository.save(notification);
        envoyerWebSocket(saved);
        return saved;
    }

    @Override
    public NotificationTransport envoyerDemandeConfirmation(String conducteurId, String passagerId,
                                                   String trajetId, String reservationId) {
        NotificationTransport notification = NotificationTransport.builder()
                .destinataireId(conducteurId)
                .expediteurId(passagerId)
                .trajetId(trajetId)
                .reservationId(reservationId)
                .type(TypeNotification.DEMANDE_CONFIRMATION)
                .contenu("Un employé demande à rejoindre votre trajet. Confirmez ou refusez.")
                .lu(false)
                .dateCreation(LocalDateTime.now())
                .build();
        NotificationTransport saved = notificationRepository.save(notification);
        envoyerWebSocket(saved);
        return saved;
    }

    private void envoyerWebSocket(NotificationTransport notification) {
        try {
            if (notification.getDestinataireId() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + notification.getDestinataireId(),
                        notification
                );
            }
        } catch (Exception e) {
            System.err.println("WebSocket non disponible pour: " + notification.getDestinataireId());
        }
    }
}
