package tn.esprit.rh_rse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.rh_rse.entity.Notification;
import tn.esprit.rh_rse.repository.NotificationRepository;
import tn.esprit.rh_rse.service.NotificationService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public List<Notification> getMesNotifications(String idUser) {
        return notificationRepository.findByIdUserOrderByDateCreationDesc(idUser);
    }

    @Override
    public Notification marquerCommeLu(String idNotification, String idUser) {
        Notification notification = notificationRepository.findById(idNotification)
                .orElseThrow(() -> new RuntimeException("Notification introuvable"));
        
        if (!notification.getIdUser().equals(idUser)) {
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
        
        if (!notification.getIdUser().equals(idUser)) {
            throw new RuntimeException("Non autorisé");
        }
        
        notificationRepository.delete(notification);
    }
}
