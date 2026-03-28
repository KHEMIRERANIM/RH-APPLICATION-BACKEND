package tn.esprit.rh_rse.service.impl;


import lombok.RequiredArgsConstructor;
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
        return notificationRepository.save(notification);
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

    // ─── Notification simple ───────────────────────────────
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
        return notificationRepository.save(notification);
    }

    // ─── Demande confirmation au conducteur ────────────────
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
        return notificationRepository.save(notification);
    }
}